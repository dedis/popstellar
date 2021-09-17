# PoP Android Frontend

This repository contains the UI side implementation of the PoP project.

## Table of contents

* [Architecture](#architecture) 
* [Coding Standards](#coding-standards)

## Getting Started

We assume that you're familiar with the PoP project. Please read the
[Architecture Specifications](https://docs.google.com/document/d/19r3rP6o8TO-xeZBM0GQzkHYQFSJtWy7UhjLhzzZVry4)
to get an idea about all the actors and components in the system.

## Project Structure

The project is organized into different modules as follows

```
.app/src
├── main                        # 
│    ├── java/X                 # 
│    │    ├── model             # module containing the data model & objects
│    │    │    ├── network      # ... of the objects sent over the network
│    │    │    └── objects      # ... of the application's entities
│    │    │    
│    │    ├── repository        # module 
│    │    │    ├── local        # ... of the objects sent over the network
│    │    │    └── remote       # ... of the objects sent over the network
│    │    │    
│    │    ├── ui                # module containing the application's screens
│    │    │    
│    │    └── utility           #
│    │         └── objects      # ... of the application's entities
│    │    
│    └── res                    # resources (layouts, strings, dimensions, etc.)
│
├── prod                        # 
│
├── mock                        # 
│
├── androidTest                 # 
│
├── test                        # 
│
│
│
├── network                    # module to network with the backend
│
├── parts                      # module containing the application's unique screens
│
├── res                        # resources (assets, images, etc.)
│
├── store                      # module dealing with application state (Redux-based)
│    ├── reducers              # module containing the reducers (Redux)
│    └── stores                # module to access states outside the React environment
│
├── styles                     # stylesheets
└── utils                      # interfaces to system libraries
     └── __mocks__             # mocks of those libraries, to be used in tests
```

## Architecture

We assume with the [User Interface Specifications](https://docs.google.com/document/d/1aVsCYj1vrTxh-V6CBrU4tRGANrC2wve47MSIXPliVbU/edit) file.

### Design

The application follows the Model-View-ViewModel (MVVM) pattern and uses this [guide to app architecture](https://developer.android.com/jetpack/guide). 

- The **View** consists of all the activities and fragments of the application. In this project there are only two activities respectively representing the detail and home view. The fragments represent a part of the UI within one of those activities. The packages are separated by views, for example the fragment for launching a LAO is in the home package. 

- The **ViewModel** implements the UI logic and prepares and manages the data used by the activities and fragments. We use [LiveData](https://developer.android.com/topic/libraries/architecture/livedata), an observable data holder, to keep the UI data updated. In this project each activity has its own view model.
- The **Model** consists of the local and remote data source, the model classes and the repository. The repository eases the data retrieval for the rest of the application. It is a mediator between the view model and the different data sources. In this application there are two data sources, the database and the PoP backend. The [Room](https://developer.android.com/reference/androidx/room/package-summary) persistence library is used to define the application's database, the entities and Data Access Object. 

Below is the diagram from the [guide to app architecture](https://developer.android.com/jetpack/guide) written to fit this project.

<div align="center">
  <img src="images/mvvm.png" alt="MVVM"/>
</div>

### Message definitions

All objects referred to in the protocol specification are defined in `model/network` package, 
closely mirroring the JSON-Schema folder structure.

The logic for parsing them is defined in `utility/json` package. 

When you need to create a new object please refer to existing message types. If the attributes name
of the object differ from the JSON-Schema 

If the name of the attributes
differ from the JSON-Schema then the `@SerializedName` annotation can be used to specify the field name.
is used to indicate how to serialize to JSON if the name of the attributes differ from the JSON-Schema.


### Communication

## Coding Standards

This project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). The [google-java-format](https://github.com/google/google-java-format) plugin allows very easy formatting.

Please ensure that you configure Android Studio to use `google-java-format`. As of now, v1.9 of the plugin requires JDK11 which is not shipped with Android Studio 4.0. Please install
the `Choose Runtime` plugin from the Marketplace and install a JDK11 runtime by double pressing shift and searching for `Choose Runtime` in the popup.

`google-java-format` does not handle import orders unfortunately. Please import the [google-style scheme](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
in Android Studio by going to `Preferences` -> `Editor` -> `Code Style` -> `Import Scheme...` on the Gear Icon for Scheme.

Finally, you may want to install `Save Actions` plugin and configure it to `Optimise Imports` and `Reformat File` on save.

## Resource Values

The values used for the UI are stored in the corresponding xml
files (colors, dimens, strings or styles) in the res/values folder.

The [R class in Android](https://stackoverflow.com/questions/4953077/what-is-the-class-r-in-android) is an auto-generated class containing the IDs of all the resources, the values can be accessed using ```R.id``` or ```getResources()```.

The strings and dimensions are divided by usage, for example all strings or dimensions used for the home view are grouped together.

## Github Actions

This project uses Github Actions as a CI, for more information go to the [workflows](https://github.com/dedis/student_21_pop/blob/master/.github/workflows/ci.yaml) of this project.

This CI builds and runs the Unit Tests. For the Android Tests, the [reactivecircus](https://github.com/ReactiveCircus/android-emulator-runner) Android Emulator is used, which is limited. There are issues finding the resource values and checking [Toast messages](https://developer.android.com/reference/android/widget/Toast) appearance.

