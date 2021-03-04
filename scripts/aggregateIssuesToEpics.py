#This script takes a list of 2-element lists as input, e.g PL-111,PL-1112
#where PL-1112 is the epic and PL-111 is a linked issue.

#This script also assumes that input is sorted by epic link (second column)
#This isn't a strict requirement but it'll make output data nicer

#Output is a list of lists where each first element of a list is an epic and 
#all subsequent elements are linked issues.
#
#Example input:
#PL-6311,PL-6155
#PL-6158,PL-6157
#PL-6221,PL-6157
#PL-6222,PL-6157
#
#Expected output:
#[
#["PL-6155","PL-6311"],
#["PL-6157","PL-6158","PL-6221","PL-6222"]]

#### PARAMS ####
#Change source and target filenames here

source = open("GSI_issueandepics.csv", "rt")
target = open("GSI_issueandepicshorizontal.txt", "w")

#### END PARAMS ####

#Initial variable values, leave them be
epickey = "PL-1"
issue = "PL-2"
epicandissues = ""
isFirstLine = True

target.write("[")

for line in source:
	print(line)
	epicinline = line.split(",")[1].strip("\n")
	issueinline = line.split(",")[0].strip("\n")
	print("epickey: " + epickey + " epicinline: " + epicinline)
	print("issueinline: " + issueinline)
	if(epicinline != epickey):
		epickey = epicinline
		if (isFirstLine == False): epicandissues += "],\n"
		isFirstLine = False
		print(epicandissues)
		target.write(epicandissues)
		epicandissues = "[\"" + epicinline + "\""
		epicandissues += ", " + "\"" + issueinline + "\""
	else:
		epicandissues += ", " + "\"" + issueinline + "\""
		
epicandissues += "]]"
target.write(epicandissues)

source.close()
target.close()