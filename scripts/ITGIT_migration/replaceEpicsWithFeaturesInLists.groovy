/*
This script is to be used in the Script Console.
It takes as input a list of lists with inner lists containing:
[Epic Key, Epic Summary, Issues linked to epic]
The result is a list of lists with inner lists containing:
[Feature Key, Issues to be linked with Parent link]
The whole list is then shown in Result tab.
Results need to be extracted as quoted strings in order to be usable in other scripts as inputs.
It is highly recommended to save the results in a separate text file.

In order to customize the script to work with any project, you just need to change:
- originInputData: that's the input data in list of lists form. Example provided. You can directly paste result data from
    extractEpicsSummariesEpicLinks.groovy script. If not, remember that double quotes (") in summaries need to be replaced
    with \\\" otherwise you'll get an error
- targetProject: that's the key of the project where epics were moved to and which will replace the original epics' project key
    and summaries.

This script does not change Jira content.

Uses Jira 8.x.x compatible JQL search
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
!!! PARAMS !!!
*/
def originInputData = [["PLSBOXT-1", "The spark jobs need to support a rerun functionality", "PLSBOXT-8", "PLSBOXT-9"], ["PLSBOXT-2", "UI security", "PLSBOXT-10"], ["PLSBOXT-3", "Operational Issues Handling and Notifications", "PLSBOXT-11", "PLSBOXT-12", "PLSBOXT-13"]]
def targetProject = "PLSBOXA"
/*
!!! END PARAMS !!!
*/

def log = Logger.getLogger("com.replaceEpicsWithFeatures")
log.setLevel(Level.DEBUG)

def issueManager = ComponentAccessor.getIssueManager()

def findIssues(String jqlQuery) {
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser);
    def searchProvider = ComponentAccessor.getComponent(SearchProvider);
    def query = jqlQueryParser.parseQuery(jqlQuery);
    def searchQuery = SearchQuery.create(query, user);

    def results = searchProvider.search(searchQuery, PagerFilter.getUnlimitedFilter());
    //log.warn "issue count: ${results.getTotal()}";
    results.getResults().collect { res ->
        def doc = res.getDocument();
        def key = doc.get("key");
        def issue = ComponentAccessor.getIssueManager().getIssueObject(key);
        return issue;
    }
}

def targetOutputData= []

originInputData.each{ originRow ->
    def originDataWithTargetKeys = []
    //This JQL query uses Enhanced Search provided by Adaptavist ScriptRunner plugin
    def targetJqlQuery = "issueFunction in issueFieldExactMatch(\"project = ${targetProject}\", \"summary\", \"${originRow[1]}\")"
    log.debug "targetJqlQuery: ${targetJqlQuery}"
    def targetIssueQueryResults = findIssues(targetJqlQuery)

    if(targetIssueQueryResults.size() == 0){
        log.warn "No corresponding issue found in target project for summary: ${originRow[1]}"
    }
    else if(targetIssueQueryResults.size() > 1){
        log.warn "More than 1 issue found in target project for summary: ${originRow[1]}"
    }
    else{
        def targetIssue = issueManager.getIssueObject(targetIssueQueryResults[0].getKey())
        targetIssueString = "\"" + targetIssue + "\""
        originDataWithTargetKeys << targetIssueString
        originRow.remove(0)
        originRow.remove(0)
        originRow.each{ linkedIssue ->
            def linkedIssueString = "\"" + linkedIssue + "\""
            originDataWithTargetKeys << linkedIssueString
        }
        //log.debug "sourceAndTargetKeys: ${sourceAndTargetKeys}"
    }
    targetOutputData << originDataWithTargetKeys
}

return targetOutputData