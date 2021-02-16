/*
This script is meant to be run in the script console.
This script creates copies of versions from source project in the target project. This means, for
example, that versions in target project will have different IDs.
User can choose whether to copy Released and Unreleased versions only or Archived as well.

To customize the script look into the PARAMS section.

This script has an anti-duplicate measure so it's safe to rerun. However, this also means that if
a version with an identical name exists in the target project, copying will not take place.

This script DOES change Jira content so be sure what you're doing.
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.version.Version
import org.apache.log4j.Level
import org.apache.log4j.Logger

def projectManager = ComponentAccessor.getProjectManager()
def versionManager = ComponentAccessor.getVersionManager()

/*
!!! PARAMS !!!
*/
def projectSource = projectManager.getProjectObjByKey("SOURCE") // source project key here
def projectDestination = projectManager.getProjectObjByKey("TARGET") // target project key here
//Set copyArchivedVersions to true if you want to copy archived versions as well. These versions
//will be archived in the target project.
def copyArchivedVersions = true
/*
!!! END PARAMS !!!
*/

def log = Logger.getLogger("com.copyVersions")
log.setLevel(Level.DEBUG)

List<Project> projectList = new ArrayList<>()
projectList.add(projectSource)

Collection<Version> unarchivedVersionList = versionManager.getAllVersionsForProjects(projectList, false)
Collection<Version> archivedVersionList = versionManager.getVersionsArchived(projectList)

Version versionToArchive

for(version in unarchivedVersionList) {
    //Anti-duplicate check
    if(!versionManager.getVersion(projectDestination.id, version.name)){
        log.debug("Now adding version " + version + " to " + projectDestination.name)
        versionManager.createVersion(version.name, version.startDate, version.releaseDate, version.description, projectDestination.id, null, version.released)
    }
    else log.debug("Version " + version + " already exists in project " + projectDestination.name + ". Aborting addition.")
}

if(copyArchivedVersions) {
    for (version in archivedVersionList) {
        //Anti-duplicate check
        if (!versionManager.getVersion(projectDestination.id, version.name)) {
            log.debug("Now adding archived version " + version + " to " + projectDestination.name)
            versionManager.createVersion(version.name, version.startDate, version.releaseDate, version.description, projectDestination.id, null, version.released)
            versionToArchive = versionManager.getVersion(projectDestination.id, version.name)
            versionManager.archiveVersion(versionToArchive, true)
        } else log.debug("Version " + version + " already exists in project " + projectDestination.name + ". Aborting addition.")
    }
}