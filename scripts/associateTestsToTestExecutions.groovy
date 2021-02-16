/*
 This is an example of a script used to associate test with test executions. It is used as a "Custom script post-function".
 For this post-function type there is only the Inline script field to be filled, so all actions need to be defined here, including conditions to trigger the script or not.

 Associating a test to a test execution, which is an Xray add-on feature, is a bit tricky as it cannot be done by simply manipulating issue field values.
 Instead, a REST call needs to be made, as suggested by creators of Xray here:
 https://confluence.xpand-it.com/display/XRAY/Extending+and+integrating+with+Xray+using+ScriptRunner#ExtendingandintegratingwithXrayusingScriptRunner-CreateaTestExecution
 Expand source of create_test_execution.groovy

 Following script uses a simplified version of aforementioned script, e.g. association method associates only one test to a test execution instead of a list of tests.
 This script is executed during an epic issue's Draft --> Refinement transition.

 */



import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.acme.CreateSubtask")
log.setLevel(Level.DEBUG)

def issueManager = ComponentAccessor.getIssueManager();
def epicIssueKey = issueManager.getIssueObject(issue.getKey()).getKey();
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
def searchProvider = ComponentAccessor.getComponent(SearchProvider.class);
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
def summaryCorePLPortal = "PL Portal";
def summaryCorePLEngine = "PL Engine and Flow Runner";

//Method which creates a JQL query
Object jqlQuery(type,epicKey,summary){
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
    return jqlQueryParser.parseQuery("project = PL AND type = '${type}' AND 'Epic Link' = ${epicKey} AND summary ~ '${summary}' ORDER BY key DESC");
}

//Method which returns an issue key from running a JQL query
//Note it only returns one key (the 'top' one) as we expect to get only one key from the search
String jqlResultKey(jqlQuery){
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
    def searchProvider = ComponentAccessor.getComponent(SearchProvider.class);
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    def result = searchProvider.search(jqlQuery, user, PagerFilter.getUnlimitedFilter());
    if (result.getIssues()[0]) return result.getIssues()[0].getKey();
    else return null;
}

/*
log.debug("PL Engine exec" + jqlResultKey(jqlQuery("Test Execution", epicIssueKey, summaryCorePLEngine)));
log.debug("PL Engine test" + jqlResultKey(jqlQuery("Test", epicIssueKey, summaryCorePLEngine)));
log.debug("PL Portal exec" + jqlResultKey(jqlQuery("Test Execution", epicIssueKey, summaryCorePLPortal)));
log.debug("PL Portal test" + jqlResultKey(jqlQuery("Test", epicIssueKey, summaryCorePLPortal)));
*/

//Verify if a Test Execution and a Test issue exist for PL Engine
if( jqlResultKey(jqlQuery("Test Execution", epicIssueKey, summaryCorePLEngine)) && jqlResultKey(jqlQuery("Test", epicIssueKey, summaryCorePLEngine)) ){
    associateTestsToTestExecution(jqlResultKey(jqlQuery("Test Execution", epicIssueKey, summaryCorePLEngine)),jqlResultKey(jqlQuery("Test", epicIssueKey, summaryCorePLEngine)));
}
else{
    log.debug("At least one PL Engine issue not found");
}

//Verify if a Test Execution and a Test issue exist for PL Portal
if(jqlResultKey(jqlQuery("Test Execution", epicIssueKey, summaryCorePLPortal)) && jqlResultKey(jqlQuery("Test", epicIssueKey, summaryCorePLPortal))){
    associateTestsToTestExecution(jqlResultKey(jqlQuery("Test Execution", epicIssueKey, summaryCorePLPortal)),jqlResultKey(jqlQuery("Test", epicIssueKey, summaryCorePLPortal)));
}
else{
    log.debug("At least one PL Portal issue not found");
}

/*
-----------------
This is an another way of dealing with the problem:
Here we just search for tests to be assigned to a test execution
-----------------

Object jqlQuery(type,epicKey){
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
    return jqlQueryParser.parseQuery("project = PL AND type = '${type}' AND 'Epic Link' = ${epicKey} ORDER BY key DESC");
}

Object jqlQueryWithLabel(type,epicKey,label){
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
    return jqlQueryParser.parseQuery("project = PL AND type = '${type}' AND 'Epic Link' = ${epicKey} AND labels = '${label}' ORDER BY key DESC");
}

def testResults = searchProvider.search(jqlQuery("Test", epicIssueKey), user, PagerFilter.getUnlimitedFilter())
def testExecResults = searchProvider.search(jqlQueryWithLabel("Test Execution", epicIssueKey, "EpicTestExec"), user, PagerFilter.getUnlimitedFilter())

log.debug("test execution: " + testExecResults.getIssues()[0].getKey())

testResults.getIssues().each{
    log.debug("Test issue no: " + it.getKey())
    associateTestsToTestExecution(testExecResults.getIssues()[0].getKey(), it.getKey())
}
*/

//The actual association method which makes a POST call to Xray's REST API
boolean associateTestsToTestExecution(testExecKey,testKey){
    def log = Logger.getLogger("com.acme.CreateSubtask")
    log.setLevel(Level.DEBUG)
    def url;
    def jiraBaseUrl = com.atlassian.jira.component.ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
    def endpointUrl = "${jiraBaseUrl}/rest/raven/1.0/api/testexec/${testExecKey}/test"
    url = new URL(endpointUrl);
    def requestBody = '{"add": ["' + testKey + '"]}';

    //log.debug(endpointUrl);
    //log.debug(requestBody);

    //A specific user is needed for this REST call. In this case, we use a technical user
    def username = "PLENG.Technical"
    def password = ""
    def authString = "${username}:${password}".bytes.encodeBase64().toString()

    URLConnection connection = url.openConnection();
    connection.doOutput = true
    connection.addRequestProperty("Authorization", "Basic ${authString}")
    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());
    output.write(requestBody);
    output.close();
    connection.connect();

    HttpURLConnection httpConnection = (HttpURLConnection) connection;
    log.debug("Response Code:" + httpConnection.getResponseCode());

    if (httpConnection.getResponseCode() == 200) {
        // OK
        return true;
    } else {
        // error
        return false;
    }
}