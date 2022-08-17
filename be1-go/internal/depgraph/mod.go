// Package main provides a utility CLI to generate the dependency graph of each
// package inside a module.
// You can get the explanations on how to use it with `go build && ./depgraph
// help`.
package main

import (
	"fmt"
	"go/parser"
	"go/token"
	"io"
	"log"
	"os"
	"path"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"

	"github.com/urfave/cli/v2"
	"golang.org/x/xerrors"
	"gopkg.in/yaml.v2"
)

type config struct {
	Modname    string   `yaml:"modname"`
	Includes   []string `yaml:"includes"`
	Excludes   []string `yaml:"excludes"`
	Interfaces []string `yaml:"interfaces"`
	WithTests  bool     `yaml:"withtests"`
	OverWrite  bool     `yaml:"overwrite"`
	OutFile    string   `yaml:"outfile"`
}

type bag map[string]struct{}

func main() {

	app := &cli.App{
		Name:      "depgraph",
		Usage:     "generate a dot graph",
		UsageText: "./depgraph [--config | --modname ] source ",
		Description: `This utility will recursively parse a folder and extract 
for each package that it finds the list of dependencies it uses to generate a
graphviz representation. By default it excludes _test.go files.
Since there might be a lot of dependencies, one can provide a yaml config file
in order to scope the parsing. The config format is the following:

modname: MODULE_NAME
overwrite: [true|false]
outfile: FILE_PATH.dot
withtests: [true|false]
includes:
	- go.dedis.ch/dela/*
	- ...
excludes:
	- go.dedis.ch/dela/core/.*(types|json)
	- ...
interfaces:
	- core/validation
	- ...

"includes" and "excludes" are two lists of regular expressions.

If "includes" is empty then everything is included. Otherwise, the program only
keeps the package AND dependencies that are specified in the includes list.

Each package AND dependency is checked against the "excludes" list and discarded
if it matches any of the elements.

"interfaces" is used to mark specific packages that should be displayed
differently. In this case those package will be outlined by a green
background.

Packages and their dependencies are sorted and the graph built accordingly.

Examples:

./depgrah --modname "go.dedis.ch/dela" -o graph.dot -F ./
./depgrah --config internal/depgraph/dep.yml -o graph.dot -F ./

The following commands can be used to generate a visual representation from the
output of depgraph using DOT:

dot -Tpdf graph.dot -o graph.pdf
dot -Gdpi=300 -Tpng graph.dot -o graph.png -Gsplines=ortho`,
		Flags: []cli.Flag{
			&cli.StringFlag{
				Name:    "config",
				Aliases: []string{"c"},
				Usage:   "the path to a yaml config file",
			},
			&cli.StringFlag{
				Name: "modname",
				Usage: "the module name, convenient if one doesn't want to " +
					"provide a config file. Is not taken into account if a " +
					"config with 'modname' is provided. If provided without " +
					"a config, must have a trailing '/'",
			},
			&cli.StringFlag{
				Name:    "out",
				Aliases: []string{"o"},
				Usage: "if provided, will save the result to the specified " +
					"file. Is not taken into account if a config with " +
					"'outfile' is provided.",
			},
			&cli.BoolFlag{
				Name:    "force",
				Aliases: []string{"F"},
				Usage: "overwrites the output file. Is not taken into " +
					"account if a config with 'overwrite' is provided.",
			},
			&cli.BoolFlag{
				Name:    "withTests",
				Aliases: []string{"t"},
				Usage: "includes the test files. Is not taken into account " +
					"if a config with 'withtests' is provided.",
			},
		},
		Action: run,
	}

	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}
}

// run is the main action of the CLI.
func run(c *cli.Context) error {
	searchDir := c.Args().First()
	if searchDir == "" {
		return xerrors.Errorf("please provide the folder path")
	}

	config, err := loadConfig(c)
	if err != nil {
		return xerrors.Errorf("failed to load config: %v", err)
	}

	out, err := getWriter(config)
	if err != nil {
		return xerrors.Errorf("failed to get writer: %v", err)
	}

	interfaces := make(bag)
	for _, it := range config.Interfaces {
		interfaces[it] = struct{}{}
	}

	// links will contain, for every package, a bag of dependencies.
	links := make(map[string]bag)

	err = filepath.Walk(searchDir, walkFn(config, links))
	if err != nil {
		return xerrors.Errorf("failed to parse folder: %v", err)
	}

	displayGraph(out, links, interfaces)

	return nil
}

func loadConfig(c *cli.Context) (config, error) {
	config := config{
		Modname:   c.String("modname"),
		WithTests: c.Bool("withTests"),
		OverWrite: c.Bool("force"),
		OutFile:   c.String("out"),
	}

	configPath := c.String("config")
	if configPath != "" {
		configBuf, err := os.ReadFile(configPath)
		if err != nil {
			return config, xerrors.Errorf("failed to read config file: %v", err)
		}

		err = yaml.Unmarshal(configBuf, &config)
		if err != nil {
			return config, xerrors.Errorf("failed to unmarshal config: %v", err)
		}

		// we add a "/" to build the full package name. If the module name is
		// mod.ch/module, then a package 'pancake' inside it should be
		// mod.ch/module/pancake, but the parsing will only extract 'pancake'.
		config.Modname = config.Modname + "/"
	}

	return config, nil
}

func getWriter(config config) (io.Writer, error) {
	if config.OutFile == "" {
		return os.Stdout, nil
	}

	_, err := os.Stat(config.OutFile)
	if !os.IsNotExist(err) && !config.OverWrite {
		return nil, xerrors.Errorf("file '%s' already exist, use '-F' to "+
			"overwrite", config.OutFile)
	}

	err = os.MkdirAll(path.Dir(config.OutFile), 0755)
	if err != nil {
		return nil, xerrors.Errorf("failed to create dir: %v", err)
	}

	out, err := os.Create(config.OutFile)
	if err != nil {
		return nil, xerrors.Errorf("failed to create output file: %v", err)
	}

	return out, nil
}

// walkFn returns the functions that will be called recursively on each file and
// folder
func walkFn(config config, links map[string]bag) filepath.WalkFunc {
	return func(path string, f os.FileInfo, err error) error {
		fset := token.NewFileSet()

		if err != nil {
			return xerrors.Errorf("got an error while walking: %v", err)
		}

		// we exclude the dir and non-go files
		if f.IsDir() || !strings.HasSuffix(f.Name(), ".go") ||
			strings.HasSuffix(f.Name(), "_test.go") {

			return nil
		}

		// we exclude test files if not otherwise asked
		if !config.WithTests && strings.HasSuffix(f.Name(), "_test.go") {
			return nil
		}

		astFile, err := parser.ParseFile(fset, path, nil, parser.ImportsOnly)
		if err != nil {
			return xerrors.Errorf("failed to parse file: %v", err)
		}

		path = filepath.Dir(path)
		// This is the full package path. From "mino" we want
		// "go.dedis.ch/dela/mino"
		packagePath := config.Modname + path

		if !isIncluded(packagePath, config.Includes) ||
			isExcluded(packagePath, config.Excludes) {
			return nil
		}

		for _, s := range astFile.Imports {
			// because an import path is always surrounded with "" we remove
			// them
			importPath := s.Path.Value[1 : len(s.Path.Value)-1]

			if !isIncluded(importPath, config.Includes) ||
				isExcluded(importPath, config.Excludes) {

				continue
			}

			// in the case the package imports a package from the same module,
			// we want to keep only the "relative" name. From
			// "go.dedis.ch/dela/mino/minogrpc" we want only "mino/minogrpc".
			importPath = strings.TrimPrefix(importPath, config.Modname)

			if links[packagePath[len(config.Modname):]] == nil {
				links[packagePath[len(config.Modname):]] = make(bag)
			}

			// add the dependency to the bag
			links[packagePath[len(config.Modname):]][importPath] = struct{}{}
		}

		return nil
	}
}

func displayGraph(out io.Writer, links map[string]bag, interfaces bag) {
	// a bag of nodes, used to keep track of every node added so that we can
	// later on outline the interfaces.
	nodesList := make(bag)

	fmt.Fprintf(out, "strict digraph {\n")
	fmt.Fprintf(out, "labelloc=\"t\";\n")
	fmt.Fprintf(out, "label = <Modules dependencies of popstellar "+
		"<font point-size='10'><br/>(generated %s)</font>>;\n",
		time.Now().Format("2 Jan 06 - 15:04:05"))
	fmt.Fprintf(out, "graph [fontname = \"helvetica\"];\n")
	fmt.Fprintf(out, "graph [fontname = \"helvetica\"];\n")
	fmt.Fprintf(out, "node [fontname = \"helvetica\"];\n")
	fmt.Fprintf(out, "edge [fontname = \"helvetica\"];\n")
	fmt.Fprintf(out, "node [shape=box,style=rounded];\n")
	// To have (more or less) deterministric result
	fmt.Fprintf(out, "start=0;\n")
	fmt.Fprintf(out, "ratio = fill;\n")
	fmt.Fprintf(out, "rankdir=\"LR\";\n")

	// We sort packages to improve the rendering
	packages := make([]string, 0, len(links))
	for pkg := range links {
		packages = append(packages, pkg)
	}

	sort.Strings(packages)

	for _, pkg := range packages {
		depsBag := links[pkg]
		nodesList[pkg] = struct{}{}

		// We sort dependencies to improve the rendering
		dependencies := make([]string, 0, len(depsBag))
		for dep := range depsBag {
			dependencies = append(dependencies, dep)
		}

		sort.Strings(dependencies)

		for _, dep := range dependencies {
			nodesList[dep] = struct{}{}
			fmt.Fprintf(out, "\"%v\" -> \"%v\" [minlen=1];\n", pkg, dep)
		}
	}

	// outlines the interface nodes
	for k := range nodesList {
		_, found := interfaces[k]
		if found {
			fmt.Fprintf(out, "\"%s\" [style=filled fillcolor=olivedrab1];\n", k)
		}
	}

	fmt.Fprintf(out, "}\n")
}

func isIncluded(path string, includes []string) bool {
	if len(includes) == 0 {
		return true
	}

	return matchSlice(path, includes)
}

func isExcluded(path string, excludes []string) bool {
	return matchSlice(path, excludes)
}

func matchSlice(el string, slice []string) bool {
	for _, e := range slice {
		reg := regexp.MustCompile(e)

		ok := reg.MatchString(el)
		if ok {
			return true
		}
	}

	return false
}
