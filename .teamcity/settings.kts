
import CommonSteps.buildAndTest
import CommonSteps.createParameters
import CommonSteps.printPullRequestNumber
import CommonSteps.runSonarScript
import jetbrains.buildServer.configs.kotlin.BuildType
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

version = "2022.04"

var project = Project {
    vcsRoot(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsMaster)
    vcsRoot(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsPR)
    buildType(Build)
    buildType(PullRequestBuild)
}

object Build : BuildType({
    name = "Master Build"

    vcs {
        root(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsMaster)
        cleanCheckout = true
        excludeDefaultBranchChanges = true
    }

    params {
        param("git.branch.specification", "")
    }

    createParameters()

    printPullRequestNumber()

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
        root(HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsPR)
        cleanCheckout = true
        excludeDefaultBranchChanges = true
    }

    params {
        param("git.branch.specification", "+:refs/pull/*/merge")
    }
    createParameters()

    printPullRequestNumber()

    buildAndTest()

    runSonarScript()

    triggers {
        vcs {
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsPR.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:3a001759-7d6f-4348-9d4d-2eb43f7b51ad"
                }
            }
        }
        pullRequests {
            vcsRootExtId = "${HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsPR.id}"
            provider = github {
                authType = token {
                    token = "credentialsJSON:3a001759-7d6f-4348-9d4d-2eb43f7b51ad"
                }
                filterSourceBranch = "refs/pull/*/merge"
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
            }
        }
    }
})

object HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsMaster : GitVcsRoot({
    name = "Master Build"
    url = "https://github.com/jpspringall/team-city-sonar-cube"
    branch = "refs/heads/master"
    agentCleanPolicy = GitVcsRoot.AgentCleanPolicy.ALWAYS
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "jpspringall"
        password = "credentialsJSON:e224d815-b2d6-4dc7-9e5c-11f7d85dbd51"
    }
    param("oauthProviderId", "PROJECT_EXT_2")
})

object HttpsGithubComJpspringallTeamCitySonarCubeRefsHeadsPR : GitVcsRoot({
    name = "Pull Request Build"
    url = "https://github.com/jpspringall/team-city-sonar-cube"
    branch = "refs/heads/master"
    branchSpec = "%git.branch.specification%"
    //branchSpec = "refs/pull/*/head"
    agentCleanPolicy = GitVcsRoot.AgentCleanPolicy.ALWAYS
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "jpspringall"
        password = "credentialsJSON:e224d815-b2d6-4dc7-9e5c-11f7d85dbd51"
    }
    param("oauthProviderId", "PROJECT_EXT_2")
})

for (bt : BuildType in project.buildTypes ) {
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