/*
 This is an example of a script used to create a number of sub-tasks of another issue. This example is used with a "Custom script post-function" post-function type.
 For this post-function type there is only the Inline script field to be filled, so all actions need to be defined here, including conditions to trigger the script or not.

 This particular example is meant to create 9 sub-tasks for a Release Deployment task created in a previously executed (or already existing) post-function.
 It is executed during an epic's Draft --> Refinement transition

*/

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Logger
import org.apache.log4j.Level

//Setting up a Logger.
//NOTE: tutorials suggest that defining log is not necessary which is contrary to my experience.
def log = Logger.getLogger("com.acme.CreateSubtask")
log.setLevel(Level.DEBUG)

//Lots of defs
def constantManager = ComponentAccessor.getConstantsManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def issueFactory = ComponentAccessor.getIssueFactory()
def subTaskManager = ComponentAccessor.getSubTaskManager()
def issueManager = ComponentAccessor.getIssueManager()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
def searchProvider = ComponentAccessor.getComponent(SearchProvider.class)
//Get Epic issue object. issue variable is associated with epic because this script is run during an Epic's status transition
def epicIssue = issueManager.getIssueObject(issue.getKey());

//Finding parent task ID - we expect to have only one result from this search query due to previous post function's conditions
def parentTaskJqlQuery = jqlQueryParser.parseQuery("project = PL AND type = Task AND 'Epic Link' = ${epicIssue.getKey()} AND summary ~ 'Release Deployment' ORDER BY key DESC");
def parentTaskResults = searchProvider.search(parentTaskJqlQuery, user, PagerFilter.getUnlimitedFilter());
def Issue parentTaskIssue;

//If parent task issue exists, proceed further. If not, end script.
if(parentTaskResults.getIssues()[0]){
    parentTaskIssue = parentTaskResults.getIssues()[0];
}
else{
    log.debug("Sub-tasks creation stopped because target parent task does not exist")
    return;
}

//Don't create sub-tasks for task if they already exist (or actually, 'Validate Release Steps' in particular)
def subtasksExistJqlQuery = jqlQueryParser.parseQuery("project = PL AND type = Sub-Task AND parent = ${parentTaskIssue.getKey()} AND summary ~ 'Validate Release Steps' ORDER BY key DESC");
def subtasksExistResults = searchProvider.search(subtasksExistJqlQuery, user, PagerFilter.getUnlimitedFilter());
if(subtasksExistResults.getIssues()[0]){
    log.debug("Creation of Replease Deployment sub-tasks stopped because they already exist");
    return;
}

def summariesList = ["Prepare Test Plan and Test Report",
                     "Prepare release candidate for PL Engine",
                     "Prepare release candidate for PL Portal",
                     "Prepare Remedy requests",
                     "Validate Release Steps",
                     "Update Configurations",
                     "Prepare UC4 changes remedies for UAT / PREPROD / PROD",
                     "Create PL Release page",
                     "Check JIRAs for content and labels"];

//For each defined summary, create a sub-task and create a SubTask issue link to parent task
summariesList.each {
    //Set subTask properties
    MutableIssue newSubTask = issueFactory.getIssue();
    newSubTask.setSummary(it);
    newSubTask.setParentObject(parentTaskIssue);
    newSubTask.setProjectObject(parentTaskIssue.getProjectObject());
    newSubTask.setIssueTypeId(constantManager.getAllIssueTypeObjects().find{
        it.getName() == "Sub-task"
    }.id);

    def newIssueParams = ["issue" : newSubTask] as Map<String,Object>;

    //Create subtask
    issueManager.createIssueObject(user, newIssueParams);
    //Link subtask to parent task
    subTaskManager.createSubTaskIssueLink(parentTaskIssue, newSubTask, user);

    log.info "Issue with summary ${newSubTask.summary} created";

}