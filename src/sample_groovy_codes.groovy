// Groovy script to get job's build numbers 
import jenkins.model.*
import hudson.model.*
import hudson.model.Result
import hudson.util.RunList

def job = Jenkins.getInstance().getItem("MediConnekt-DEV")
RunList<?> builds = job.getBuilds().overThresholdOnly(Result.SUCCESS)
def list = builds.limit(10).collect{ it.number }
