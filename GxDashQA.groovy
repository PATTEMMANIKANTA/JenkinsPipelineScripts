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
						   locations: [[credentialsId: '57901973-a218-4080-8c52-042267271b8e', 
						   depthOption: 'infinity', 
						   ignoreExternalsOption: true, 
						   local: '.', 
						   remote: "https://10.3.10.16/svn/GxApps/branches/QA/GxDash"]], 
			  workspaceUpdater: [$class: 'UpdateUpdater']])
		  slackSend (color: '#00FF00', message: "SVN CHECKOUT SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
	}catch(Exception err)
    {
        //slackSend (color: '#FF0000', message: "SVN CHECKOUT FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
	
		currentBuild.result = 'FAILURE'

        mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "SVN CHECKOUT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
    }
    
    try
	{
		stage('Gradle build and Unit test')
		{
		  
		    build job: 'GxDash_QA_War', parameters: [booleanParam(name: 'Build', value: true)]
			
		    build job: 'Parsers_QA_Jar', parameters: [booleanParam(name: 'Build', value: true)]
			
				
		  slackSend (color: '#00FF00', message: "GRADLE BUILD SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "GRADLE BUILD FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		
		currentBuild.result = 'FAILURE'

        mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "MAVEN BUILD failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
	}
	
	try
	{
		stage('Sonar scan execution') 
		{ 
			withSonarQubeEnv('Sonar') 
			{ 
			  echo'**************Sonar Scan Execution Started***********************'    
			  
			  //sh '/opt/gradle/gradle-3.4.1/bin/gradle ParserPersisterJar war sonarqube -Dsonar.host.url=http://10.1.20.75:9000 -Dsonar.login=admin -Dsonar.password=admin -x test -Dsonar.svn.username=gxdashadmin -Dsonar.scm.provider=svn -Dsonar.svn.password.secured=Galaxy135'
			  
			  echo'**************Sonar Scan Execution ended*************************'
			}
			slackSend (color: '#00FF00', message: "SONAR SCAN SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "SONAR SCAN FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		currentBuild.result = 'FAILURE'

        mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "SONAR SCAN failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
	}
	
	
	try
	{
		stage('QA Deployment')
		{
			echo "********************Archiving Old Code Started********************"
			
			sh 'ssh svcdevops@10.3.10.94 "7za.exe a E:/backup/GxDash_${BUILD_NUMBER}_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip E:/Tools/Tomcat-8.5.13/webapps/GxDash.war"'
			
			echo "********************Archived Old Code********************"
			
			try{
				echo "stop server"
				
				sh 'ssh svcdevops@10.3.10.94 "SC STOP Tomcat"'
				
			}catch(Exception err){
			
				echo "Tomcat not running"
			}
			
			sleep 10
			
			echo "delete warfile and GxDash,logs floders"
			
			sh 'ssh svcdevops@10.3.10.94 "del E:\\Tools\\Tomcat-8.5.13\\webapps\\GxDash.war && del /F /Q E:\\Tools\\Tomcat-8.5.13\\webapps\\GxDash && del /F /Q E:\\Tools\\Tomcat-8.5.13\\logs"'
									
			echo "copying warfile to the server"			
			
			sh 'scp /home/jenkins/workspace/GxDash_QA_War/build/libs/GxDash.war svcdevops@10.3.10.94:E:/Tools/Tomcat-8.5.13/webapps/'

			echo "Restarting tomcat"
			
			sh 'ssh svcdevops@10.3.10.94 "SC START Tomcat"'			
			
			mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

			cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

			subject: "QA DEPLOYMENT FINISHED - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

			body: "Please go to ${env.BUILD_URL}.");
                             
		}
	}catch(Exception err)
	{
				
		emailext attachLog: true, body: 'Please find the attached build log for GxDash.', 
				 recipientProviders: [[$class: 'DevelopersRecipientProvider']], 
				 subject: 'QA DEPLOYMENT failed', 
				 to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com'
		
		echo 'DEPLOYING PREVIOUS CODE'
		
		sh 'ssh svcdevops@10.3.10.94 "7za.exe e "E:/backup/GxDash_${BUILD_NUMBER}_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip" -oE:/Tools/Tomcat-8.5.13/webapps/ -y -r"'
					
		echo "Restarting Tomcat"
								
		sh 'ssh svcdevops@10.3.10.94 "SC START Tomcat"'
			
		currentBuild.result = 'FAILURE'
		
        mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "GxDash QA DEPLOYMENT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");
		
		

                                throw err
	}
}