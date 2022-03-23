# Depgraph

This tool generates a graph of packages dependencies. The the `be1-go` folder,
run with:

```sh
go run ./internal/depgraph/ --config internal/depgraph/dep.yml -o graph.dot -F ./
```

This will generate a `.dot` file containing the graph definition in plain text.
You can convert it to a more convenient format (PDF or PNG for example) with the
`dot` utility, if installed:

```sh
dot -Tpng graph.dot -o graph.png
```