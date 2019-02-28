node('master')
{
    try
	{
		stage('Code checkout')
		{
            checkout([$class: 'SubversionSCM', 
			  additionalCredentials: [],
			  excludedCommitMessages: '', 
			  excludedRegions: '', 
			  excludedRevprop: '', 
			  excludedUsers: '', 
			  filterChangelog: false, 
			  ignoreDirPropChanges: false, 
			  includedRegions: '', 
						   locations: [[credentialsId: 'e2ea7b90-ebd2-4a00-b067-90325343f924', 
						   depthOption: 'infinity', 
						   ignoreExternalsOption: true, 
						   local: '.', 
						   remote: "https://10.3.10.16/svn/GxApps/GxQA/Demo/GxQAHubDemo"]], 
			  workspaceUpdater: [$class: 'UpdateUpdater']])
		  slackSend (color: '#00FF00', message: "SVN CHECKOUT SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
	}catch(Exception err)
    {
        slackSend (color: '#FF0000', message: "SVN CHECKOUT FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		currentBuild.result = 'FAILURE'

        mail (to: 'seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com',

        subject: "SVN CHECKOUT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                exit
    }
    
    try
	{
		stage('Maven build and Unit test')
		{
		  sh '/home/skumar/apache-maven-3.5.2/bin/mvn clean install package'
				if (currentBuild.currentResult == 'FAILURE')
				{
					echo 'Maven Build was Failed'
					// notify users when the build is back to normal
				//	mail to: email@gmail.com,
					//	subject: "Build fixed: ${currentBuild.fullDisplayName}",
					//	body: "The build is back to normal ${env.BUILD_URL}"
				}
		 slackSend (color: '#00FF00', message: "MAVEN BUILD SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "MAVEN BUILD FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		currentBuild.result = 'FAILURE'

        mail (to: 'seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com',

        subject: "MAVEN BUILD failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                exit
	}
	
	try
	{
		stage('Sonar scan execution') 
		{ 
			
		}
	}catch(Exception err)
	{
		
	}
	
	try
	{
		
	}catch(Exception err)
	{
		
	}
	try
	{
		stage('Deployment')
		{
			
			
                             
		}
	}catch(Exception err)
	{
		
	}

		
   
	
	try
	{	
		stage('Release and publish artifact to nexus')
		{			

		}
	}
	catch (Exception err) 
	{

	}

	
	try
	{

			stage('Test results publisher')
				{
									
                	
							
				}		
				

			
	}
	catch(Exception err)
	{
		
	}
    
    try
	{
		stage('Success status notification.')
		{
			
		}
	}catch(Exception err)
	{
		
	}
}