
import CommonSteps.buildAndTest
import CommonSteps.createParameters
import CommonSteps.printDeployNumber
import CommonSteps.printPullRequestNumber
import CommonSteps.runMakeTest
import CommonSteps.runSonarScript
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

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

val builds: ArrayList<BuildType> = arrayListOf()

builds.add(MasterBuild)
builds.add(PullRequestBuild)
builds.add(DeployBuild)
builds.add(SubDeployBuild)

project {
    vcsRoot(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild)

    builds.forEach{
        buildType(it)
    }

    buildTypesOrder = builds
}

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

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_2"
            }
        }
    }
})

object PullRequestBuild : BuildType({
    name = "Pull Request Build"

    vcs {
        root(DslContext.settingsRoot)
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
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_2"
            }
        }
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRootId}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
        pullRequests {
            vcsRootExtId = "${DslContext.settingsRootId}"
            provider = github {
                authType = vcsRoot()
                filterSourceBranch = "refs/pull/*/merge"
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
            }
        }
    }
})

object SubDeployBuild : BuildType({
    name = "Sub Deploy Build"

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


object HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsBuild : GitVcsRoot({
    name = "Build VCS Root"
    url = "https://github.com/jpspringall/team-city-sonar-cube"
    branch = "refs/heads/master"
    branchSpec = "%git.branch.specification%"
    agentCleanPolicy = GitVcsRoot.AgentCleanPolicy.ALWAYS
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = token {
        userName = "oauth2"
        tokenId = "tc_token_id:CID_730d8fd765b6b9d789b1b514fdc46d66:-1:ea14ff39-5e97-44f2-81ee-884245e32f4c"
    }
})

//for (bt : BuildType in project.buildTypes ) {
//    val gitSpec = bt.params.findRawParam("git.branch.specification")
//    if (gitSpec != null && gitSpec.value.isNotBlank()) {
//        bt.vcs.branchFilter = """
//            +:*
//            -:<default>
//        """.trimIndent()
//    }
//    if (bt.name == "Pull Request Build" || bt.name == "Master Build") {
//        bt.features.add {
//            feature {
//                type = "xml-report-plugin"
//                param("verbose", "true")
//                param("xmlReportParsing.reportType", "trx")
//                param("xmlReportParsing.reportDirs","%system.teamcity.build.checkoutDir%/test-results/**/*.trx")
//            }
//        }
//    }
//    if (bt.name == "Pull Request Build" || bt.name == "Master Build")
//    {
//        bt.features.add {  xmlReport {
//            reportType = XmlReport.XmlReportType.TRX
//            rules = "%system.teamcity.build.checkoutDir%/test-results/**/*.trx" //Remember to match this in test output
//            verbose = true
//        } }
//    }
//}
