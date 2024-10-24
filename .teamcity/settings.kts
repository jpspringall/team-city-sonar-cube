
import CommonSteps.buildAndTest
import CommonSteps.createParameters
import CommonSteps.printDeployNumber
import CommonSteps.printPullRequestNumber
import CommonSteps.runMakeTest
import CommonSteps.runSonarScript
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.FailureAction
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.add
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.version

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2024.03"

object MasterBuild : BuildType({
    name = "Master Build"

    vcs {
        root(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild)
        cleanCheckout = true
        excludeDefaultBranchChanges = true
    }

    params {
        param("git.branch.specification", "")
    }

    createParameters()

    printPullRequestNumber()

    runMakeTest()

    buildAndTest()

    runSonarScript()

    triggers {
        vcs {
        }
    }

    features {}
})

object PullRequestBuild : BuildType({
    name = "Pull Request Build"

    vcs {
        root(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild)
        cleanCheckout = true
        excludeDefaultBranchChanges = true
    }

    params {
        param("git.branch.specification", "+:refs/pull/*/merge")
    }
    createParameters()

    printPullRequestNumber()

    runMakeTest()

    buildAndTest()

    runSonarScript()

    triggers {
        vcs {
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:a75b57d5-0461-4052-b9c6-58dfd9f2ee53" // This is the PAT
                }
            }
        }
        pullRequests {
            vcsRootExtId = "${HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild.id}"
            provider = github {
                authType = token {
                    token = "credentialsJSON:a75b57d5-0461-4052-b9c6-58dfd9f2ee53" // This is the PAT
                }
                filterSourceBranch = "refs/pull/*/merge"
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
            }
        }
    }
})

object DeployBuild : BuildType({
    name = "Deploy Build"

    vcs {
        root(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild)
        cleanCheckout = true
        excludeDefaultBranchChanges = true
    }

    buildNumberPattern = MasterBuild.depParamRefs.buildNumber.toString()

    dependencies {
        snapshot(MasterBuild) {
            onDependencyFailure = FailureAction.FAIL_TO_START
            onDependencyCancel = FailureAction.CANCEL
        }
    }

    params {
        param("git.branch.specification", "")
    }

    createParameters()

    printDeployNumber()

    triggers {
        vcs {
        }
    }

    features {}
})

val builds: ArrayList<BuildType> = arrayListOf()

builds.add(MasterBuild)
builds.add(PullRequestBuild)
builds.add(DeployBuild)

val project = Project {
    vcsRoot(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild)

    builds.forEach{
        buildType(it)
    }

    buildTypesOrder = builds
}

object HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild : GitVcsRoot({
    name = "Build VCS Root"
    url = "https://github.com/jpspringall/team-city-sonar-cube"
    branch = "refs/heads/master"
    branchSpec = "%git.branch.specification%"
    agentCleanPolicy = GitVcsRoot.AgentCleanPolicy.ALWAYS
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "jpspringall"
        password = "credentialsJSON:e224d815-b2d6-4dc7-9e5c-11f7d85dbd51"
    }
})

for (bt : BuildType in project.buildTypes ) {
    bt.paused = false
    val gitSpec = bt.params.findRawParam("git.branch.specification")
    if (gitSpec != null && gitSpec.value.isNotBlank()) {
        bt.vcs.branchFilter = """
            +:*
            -:<default>
        """.trimIndent()
    }
    if (bt.name == "Pull Request Build" || bt.name == "Master Build") {
        bt.features.add {
            feature {
                type = "xml-report-plugin"
                param("verbose", "true")
                param("xmlReportParsing.reportType", "trx")
                param("xmlReportParsing.reportDirs","%system.teamcity.build.checkoutDir%/test-results/**/*.trx")
            }
        }
    }
//    if (bt.name == "Pull Request Build" || bt.name == "Master Build")
//    {
//        bt.features.add {  xmlReport {
//            reportType = XmlReport.XmlReportType.TRX
//            rules = "%system.teamcity.build.checkoutDir%/test-results/**/*.trx" //Remember to match this in test output
//            verbose = true
//        } }
//    }
}

project(project)