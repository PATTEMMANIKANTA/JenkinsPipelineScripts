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
						   locations: [[credentialsId: 'bcc2c15e-c50d-486e-936d-e9883abe59c3', 
						   depthOption: 'infinity', 
						   ignoreExternalsOption: true, 
						   local: '.', 
						   remote: "https://10.3.10.16/svn/GxCare_Claim/tags/GxClaimsV2.0/GxClaims"]], 
			  workspaceUpdater: [$class: 'UpdateUpdater']])
		  slackSend (color: '#00FF00', message: "SVN CHECKOUT SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
	}catch(Exception err)
    {
        slackSend (color: '#FF0000', message: "SVN CHECKOUT FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		currentBuild.result = 'FAILURE'

        mail (to: 'sjavvaji@galaxe.com,rsunka@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "SVN CHECKOUT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
    }
    
    	stage('Maven build')
		{
		    try
		    {
		  
		    sh '/home/skumar/maven/apache-maven-3.5.4/bin/mvn clean install package'
		  
	        }catch(Exception err)
	        {
		        slackSend (color: '#FF0000', message: "GRADLE BUILD FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		
		        currentBuild.result = 'FAILURE'

                mail (to: 'sjavvaji@galaxe.com,rsunka@galaxe.com,seramanathan@galaxe.com',

                cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

                subject: "MAVEN BUILD failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

                body: "Please go to ${env.BUILD_URL}.");

                                throw err
	        }
		}
	
	try
	{
		stage('Sonar scan execution') 
		{ 
		//	withSonarQubeEnv('Sonar') 
		//	{ 
		//	  echo'**************Sonar Scan Execution Started***********************'    
			  
		//	  sh '/opt/gradle/gradle-3.4.1/bin/gradle ParserPersisterJar war sonarqube -Dsonar.host.url=http://10.1.20.75:9000 -Dsonar.login=admin -Dsonar.password=admin -x test -Dsonar.svn.username=gxdashadmin -Dsonar.scm.provider=svn -Dsonar.svn.password.secured=Galaxy135'
			  
		//	  echo'**************Sonar Scan Execution ended*************************'
		//	}
		//	slackSend (color: '#00FF00', message: "SONAR SCAN SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "SONAR SCAN FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
	
		currentBuild.result = 'FAILURE'

        mail (to: 'sjavvaji@galaxe.com,rsunka@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "SONAR SCAN failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
	}
	
	
	try
	{
		stage('Dev Deployment')
		{
			echo'**************Development Started***********************'
			
			echo '**************Stoping Tomcat Server and removing GxClaims folder***********************'
							
			sh 'su  ansible -c "ssh rsunka@10.2.10.62 \'/home/dsah/Tomcat/apache-tomcat/bin/shutdown.sh && rm -rf /home/dsah/Tomcat/apache-tomcat/webapp/GxClaims && rm -rf /home/dsah/Tomcat/apache-tomcat/work/Catalina/localhost/GxClaims \'"'
			
			echo '**************Stopped Tomcat Server***********************'
			
			echo '**************Taking Backup of current code***********************'
				
			sh 'su  ansible -c "ssh rsunka@10.2.10.62 \'mkdir -p /home/rsunka/GxClaimsBackup/GxClaim_$(date +%F) && chmod -R 777 /home/rsunka/GxClaimsBackup/GxClaim_$(date +%F) && yes | cp /home/dsah/Tomcat/apache-tomcat/webapps/GxClaims.war /home/rsunka/GxClaimsBackup/GxClaim_$(date +%F)/\'"'
			
			sh 'chmod -R 777 /home/jenkins/workspace/GxClaims_DEV'
									
			sh 'su  ansible -c "scp /home/jenkins/workspace/GxClaims_DEV/target/GxClaims.war rsunka@10.2.10.62:/home/dsah/Tomcat/apache-tomcat/webapps"'
			
			echo '**************Start Tomcat Server***********************'
			
			sh 'su  ansible -c "ssh rsunka@10.2.10.62 "/home/dsah/Tomcat/apache-tomcat/bin/startup.sh""'			
			
			
			mail (to: 'sjavvaji@galaxe.com,rsunka@galaxe.com,seramanathan@galaxe.com',

			cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

			subject: "GxClaims DEPLOYMENT FINISHED in DEV env- Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

			body: "Please go to ${env.BUILD_URL}.");	
                             
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "DEPLOYMENT FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		
		currentBuild.result = 'FAILURE'
		
		echo "*************Deployment Failed and reverting to previous code*************"
		
		sh 'su  ansible -c "ssh rsunka@10.2.10.62 \'yes | cp /home/rsunka/GxClaimsBackup/GxClaim_$(date +%F)/GxClaims.war /home/dsah/Tomcat/apache-tomcat/webapps/ && /home/dsah/Tomcat/apache-tomcat/bin/startup.sh \'"'

        mail (to: 'sjavvaji@galaxe.com,rsunka@galaxe.com,seramanathan@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com',

        subject: "GxClaims DEPLOYMENT failed in DEV env - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
	}
}