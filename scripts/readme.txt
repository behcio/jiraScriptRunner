This python script is really simple and you need to modify the input and output filenames in its code.
Use it as preparation for migrating Epic Links, e.g. when migrating from ITCM to ITGIT.

The aim of this script is to collapse many Key-EpicLink rows to fewer EpicLink-Key1,Key2... rows. This form is required for the actual Epic Link Import script to work properly.

How to use this script? Steps:
1. Tell the person from the project that requests restoring epic links (let's call that person "project dude") to export keys and epic links to a csv file.
	a. Project dude needs to run JQL query: project = {project_key} AND issuetype not in (Epic) AND "Epic Link" is not empty
	b. Project dude needs to limit the Columns viewed to Key and Epic Link, in that order
	c. Project dude needs to export to CSV (Current fields)
		It may happen that there's too much data to export. Project dude needs to do a few exports and merge it all into one file.
	d. Project dude sends the file with all the data to you. Remember that the file needs to retain proper format. See script comments for example.
2. Go into the script aggregateIssuesToEpics.py and edit input and output filenames to your liking.
3. Execute the script
4. ????
5. PROFIT!
6. Use the output data in the importEpicLinks script