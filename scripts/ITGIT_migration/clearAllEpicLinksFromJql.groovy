/*
This script is meant to be run in the script console.
This script removes Epic Links from issues found using provided JQL Query (jqlQuery variable)
or rather, sets Epic Link to null.

In order to customize the script to work with any project, you just need to change
the jqlQuery in PARAMS section. Remember that you want to clear the epic links in issues (children),
not in the epics themselves.
Apparently this script is quite heavy for Jira to process if your input data is very large (few
thousand issues). It might be a good idea to split input data into smaller chunks (by applying more
specific filters) and execute the script a few times.

This script DOES change Jira content so be sure what you're doing.

Uses Jira 8.x.x compatible JQL search
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
!!! PARAMS !!!
*/
def jqlQuery = "project = PL and issuetype != Epic"
/*
!!! END PARAMS !!!
*/

def log = Logger.getLogger("com.ClearEpicLinks")
log.setLevel(Level.DEBUG)

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

def findIssues(String jqlQuery) {
    def issueManager = ComponentAccessor.issueManager;
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

def foundIssues = findIssues(jqlQuery)

foundIssues.each{ iss ->
    log.debug "processed issue: ${iss}"
    def processedIssue = issueManager.getIssueObject(iss.getKey())
    def epicLinkField = ComponentAccessor.customFieldManager.getCustomFieldObjects(processedIssue).findByName("Epic Link")
    epicLinkField.updateValue(null, processedIssue, new ModifiedValue(processedIssue.getCustomFieldValue(epicLinkField), null), new DefaultIssueChangeHolder())
}
