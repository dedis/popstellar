### PoP Android Frontend

This repository contains the UI side implementation of the PoP project.

#### Table of contents

* [Architecture](#architecture) 
* [Coding Standards](#coding-standards)

#### Architecture

We assume that you're familiar with the PoP project and the [User Interface Specifications](https://docs.google.com/document/d/1aVsCYj1vrTxh-V6CBrU4tRGANrC2wve47MSIXPliVbU/edit).

##### Design

The application follows the Model-View-ViewModel (MVVM) pattern and uses this [guide to app architecture](https://developer.android.com/jetpack/guide). 

- The **View** consists of all the activities and fragments of the application. In this project there are only two activities respectively representing the detail and home view. The fragments represent a part of the UI within one of those activities. The packages are separated by views, for example the fragment for launching a LAO is in the home package. 

- The **ViewModel** implements the UI logic and prepares and manages the data used by the activities and fragments. We use [LiveData](https://developer.android.com/topic/libraries/architecture/livedata), an observable data holder, to keep the UI data updated. In this project each activity has its own view model.
- The **Model** consists of the local and remote data source, the model classes and the repository. The repository eases the data retrieval for the rest of the application. It is a mediator between the view model and the different data sources. In this application there are two data sources, the database and the PoP backend. The [Room](https://developer.android.com/reference/androidx/room/package-summary) persistence library is used to define the application's database, the entities and Data Access Object. 

Below is the diagram from the [guide to app architecture](https://developer.android.com/jetpack/guide) written to fit this project.

<div align="center">
  <img src="images/mvvm.png" alt="MVVM"/>
</div>

##### Communication

#### Coding Standards

This project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). The [google-java-format](https://github.com/google/google-java-format) plugin allows very easy formatting.

Please ensure that you configure Android Studio to use `google-java-format`. As of now, v1.9 of the plugin requires JDK11 which is not shipped with Android Studio 4.0. Please install
the `Choose Runtime` plugin from the Marketplace and install a JDK11 runtime by double pressing shift and searching for `Choose Runtime` in the popup.

`google-java-format` does not handle import orders unfortunately. Please import the [google-style scheme](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
in Android Studio by going to `Preferences` -> `Editor` -> `Code Style` -> `Import Scheme...` on the Gear Icon for Scheme.

Finally, you may want to install `Save Actions` plugin and configure it to `Optimise Imports` and `Reformat File` on save.

##### Resource Values

The values used for the UI are stored in the corresponding xml
files (colors, dimens, strings or styles) in the res/values folder.

The [R class in Android](https://stackoverflow.com/questions/4953077/what-is-the-class-r-in-android) is an auto-generated class containing the IDs of all the resources, the values can be accessed using ```R.id``` or ```getResources()```.

The strings and dimensions are divided by usage, for example all strings or dimensions used for the home view are grouped together.

##### Github Actions

This project uses Github Actions as a CI, for more information go to the [workflows](https://github.com/dedis/student_21_pop/blob/master/.github/workflows/ci.yaml) of this project.

This CI builds and runs the Unit Tests. For the Android Tests, the [reactivecircus](https://github.com/ReactiveCircus/android-emulator-runner) Android Emulator is used, which is limited. There are issues finding the resource values and checking [Toast messages](https://developer.android.com/reference/android/widget/Toast) appearance.

