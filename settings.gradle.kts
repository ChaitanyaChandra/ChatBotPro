pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        flatDir { dirs(settingsDir.resolve("app/libs")) }
        google()
        mavenCentral()
        ivy {
            name = "SherpaOnnxAndroidReleases"
            url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.4")
            patternLayout {
                artifact("[artifact]-[revision].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup("com.k2fsa.sherpa.onnx")
            }
        }
    }
}

rootProject.name = "ChatBotPro"
include(":app")
