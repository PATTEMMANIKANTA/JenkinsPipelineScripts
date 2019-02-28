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
						   remote: "https://10.3.10.16/svn/GxApps/trunk/GxCapture/GxCaptureAPI/gxcaptureapi"]], 
			  workspaceUpdater: [$class: 'UpdateUpdater']])
		  slackSend (color: '#00FF00', message: "SVN CHECKOUT SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
	}catch(Exception err)
    {
        //slackSend (color: '#FF0000', message: "SVN CHECKOUT FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
	
		currentBuild.result = 'FAILURE'

        mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

        subject: "SVN CHECKOUT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
    }
    
    try
	{
		stage('Maven Build')
		{
		  
		    build job: 'GxCaptureJWTAADev', parameters: [booleanParam(name: 'Build', value: true)]
			
		    sh '/home/skumar/maven/apache-maven-3.5.4/bin/mvn clean -Dmaven.test.skip=true install'			
			
			echo'***********************Maven Build Completed***********************'  	
		  
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "GRADLE BUILD FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		
		currentBuild.result = 'FAILURE'

        mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

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
			  
			  sh '/home/skumar/maven/apache-maven-3.5.4/bin/mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar -Dsonar.login=admin -Dsonar.password=admin -Dsonar.svn.username=svcdevops -Dsonar.scm.provider=svn -Dsonar.svn.password.secured=Galaxy123'
			  
			  echo'**************Sonar Scan Execution ended*************************'
			}
			slackSend (color: '#00FF00', message: "SONAR SCAN SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "SONAR SCAN FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		currentBuild.result = 'FAILURE'

        mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

        subject: "SONAR SCAN failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
	}
	
	
	try
	{
		stage('DEV Deployment')
		{
			try{
				echo "stop API Service"
				
				sh 'ssh svcdevops_admin@10.1.20.75 "SC STOP GxCaptureAPI"'
				
			}catch(Exception err){
			
				echo "API Service is not running"
			}
			
			echo "********************Archiving Old Code Started********************"
			
			sh 'ssh svcdevops_admin@10.1.20.75 "7za.exe a C:\\Softwares\\GxCaptureDeployment\\Backup\\API\\GxCaptureWebApi_${BUILD_NUMBER}_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip C:\\Softwares\\GxCaptureDeployment\\gxcaptureapi-0.0.1-SNAPSHOT.jar"'
			
			echo "********************Archived Old Code********************"	
			
			sleep 10
			
			echo "delete old code & folders"
			
			sh 'ssh svcdevops_admin@10.1.20.75 "del /F /Q C:\\Softwares\\GxCaptureDeployment\\gxcaptureapi-0.0.1-SNAPSHOT.jar C:\\Softwares\\GxCaptureDeployment\\GxCaptureApi.zip"'
									
			echo "copying JAR to the server"

			sh '7z a GxCaptureApi.zip /home/jenkins/workspace/GxCaptureApiDev/target/gxcaptureapi-0.0.1-SNAPSHOT.jar'			
			
			sh 'scp GxCaptureApi.zip svcdevops_admin@10.1.20.75:"C:/Softwares/GxCaptureDeployment/"'
			
			echo "Extracting API Service in Deployment Server and starting Service"
			
			sh 'ssh svcdevops_admin@10.1.20.75 "7za.exe x C:\\Softwares\\GxCaptureDeployment\\GxCaptureApi.zip -oC:\\Softwares\\GxCaptureDeployment\\ -y -r && SC START GxCaptureAPI"'		
			
		//	echo "Startining API Service in Deployment Server"
			
			//sh 'ssh svcdevops_admin@10.1.20.75 "C:\\Softwares\\GxCaptureDeployment\\api.vbs"'
			
			mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

			cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

			subject: "Dev DEPLOYMENT FINISHED for GxCaptureAPI- Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

			body: "Please go to ${env.BUILD_URL}.");
                             
		}
	}catch(Exception err)
	{
				
		echo 'DEPLOYING PREVIOUS CODE'
		
		sh 'ssh svcdevops_admin@10.1.20.75 "7za.exe x C:\\Softwares\\GxCaptureDeployment\\Backup\\API\\GxCaptureWebApi_${BUILD_NUMBER}_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip -oC:\\Softwares\\GxCaptureDeployment\\ -y -r"'
					
		echo "Restarting API Service"
		
		try{
				echo "stop API Service"
				
				sh 'ssh svcdevops_admin@10.1.20.75 "SC STOP GxCaptureAPI"'
				
			}catch(Exception e){
			
				echo "API service is not running"
			}
			
		sleep 10
		
		sh 'ssh svcdevops_admin@10.1.20.75 "SC START GxCaptureAPI"'			
		
			
		currentBuild.result = 'FAILURE'
		
        mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

        subject: "GxCapture DEV DEPLOYMENT failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");
		
		

                                throw err
	}
}