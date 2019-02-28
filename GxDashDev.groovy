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
						   remote: "https://10.3.10.16/svn/GxApps/trunk/GxDash"]], 
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
		  
		    build job: 'GxDash_Dev_War', parameters: [booleanParam(name: 'Build', value: true)]
			
		    build job: 'Parsers_Dev_Jar', parameters: [booleanParam(name: 'Build', value: true)]
			
				
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
		stage('Dev Deployment')
		{
			echo'**************Development deploy approval and deployment Started***********************'
			//if (currentBuild.result == null || currentBuild.result == 'SUCCESS') 
			//	{
			    
			 //     mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

			//		cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

			//		subject: " Job '${env.JOB_NAME}' is waiting for approval in URL ${env.BUILD_URL}",

			//		body: "Please go to ${env.BUILD_URL}.");
					
			//				timeout(time: 5, unit: 'MINUTES') 
			//				{
								// you can use the commented line if u have specific user group who CAN ONLY approve
								//input message:'Approve deployment?', submitter: 'it-ops'
					
			//					input message: 'Approve deployment?';
			//				}
			//	}
			
			//input message: 'Approve deployment?';
			
			echo'**************Development deploy approval and deployment Ended***********************'
			
			slackSend (color: '#00FF00', message: "DOCKER CONTAINER APPROVAL AND DEPLOYMENT SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
			
			sh 'chmod -R 777 /home/jenkins/workspace/GxDash_Dev_War/'
			
		//	sh 'ssh ansible@10.4.10.52 "/home/GxDash/Tomcat-8.5.13/bin/shutdown.sh"'
			
		//	sh 'runuser -l ansible -c "ansible-playbook /home/ansible/ansible_scripts/gxdashansible.yml"'
			
			sh 'su  ansible -c "scp /home/jenkins/workspace/GxDash_Dev_War/build/libs/GxDash.war ansible@10.4.10.52:/home/GxDash/Tomcat-8.5.13/webapps/"'
			
		//	sh 'ssh ansible@10.4.10.52 "cd /home/GxDash/Tomcat-8.5.13/ && chown -R ansible:ansible webapps temp logs work conf && chmod -R 777 webapps temp logs work conf"'
								
			sh 'su ansible -c "ssh ansible@10.4.10.52 "/home/GxDash/Tomcat-8.5.13/bin/shutdown.sh && sleep 10""'
			
			sh 'su ansible -c "ssh ansible@10.4.10.52 \'rm -f /home/GxDash/Tomcat-8.5.13/logs/*\'"'
			
			sh 'su ansible -c "ssh ansible@10.4.10.52 "/home/GxDash/Tomcat-8.5.13/bin/startup.sh""'
			
		//	sh 'runuser -l ansible -c "scp ansible@10.4.10.52 chmod -R 777 /home/GxDash/Tomcat-8.5.13/*"'
			
			sh 'su ansible -c "ssh ansible@10.4.10.52 \'sudo chmod -R 777 /home/GxDash/Tomcat-8.5.13/\'"'
			
			sh 'su ansible -c "ssh ansible@10.4.10.52 \'sudo chmod -R 777 /home/hduser/\'"'
			
			mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

			cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

			subject: "DEV DEPLOYMENT FINISHED - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

			body: "Please go to ${env.BUILD_URL}.");
                             
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "DEPLOYMENT FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		currentBuild.result = 'FAILURE'
		
		emailext attachLog: true, body: 'Please find the attached build log for GxDash.', 
				 recipientProviders: [[$class: 'DevelopersRecipientProvider']], 
				 subject: 'DEPLOYMENT failed', 
				 to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com'

        mail (to: 'sutyagi@galaxe.com,oujjawal@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "DEPLOYMENT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");
		
		

                                throw err
	}
}