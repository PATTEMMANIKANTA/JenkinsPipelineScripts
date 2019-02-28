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
						   remote: "https://10.3.10.16/svn/GxApps/trunk/GxCapture/GxCaptureWeb_Redux"]], 
			  workspaceUpdater: [$class: 'CheckoutUpdater']])
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
		stage('NPM Build')
		{
		    echo'***********************NPM Build Started***********************'
		    
		    configFileProvider([configFile(fileId: 'b05707a3-249d-46f1-9200-c3def891709e', targetLocation: '/home/jenkins/workspace/GxCaptureDevWeb/src/services/')]) 
		    {}

		    nodejs('NODE9') 
		    {
		    
			    sh 'npm install'
			
			    sh 'npm run build'
    
            }
            
		//	configFileProvider([configFile(fileId: '66bd5357-eb7f-4fd2-94e5-c9251e30111d', targetLocation: '/home/jenkins/workspace/GxCaptureDevWeb/build/')]) 
		//	{}
			
            echo'***********************NPM Build Completed***********************'
		}
	}catch(Exception err)
	{
		slackSend (color: '#FF0000', message: "GRADLE BUILD FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
		
		currentBuild.result = 'FAILURE'

        mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

        subject: "NPM BUILD failed - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");

                                throw err
	}	
	
	try
	{
		stage('Dev Deployment')
		{
			
			try{
				echo "stop WEB Service"
				
				sh 'ssh svcdevops_admin@10.1.20.75 "SC STOP GxCaptureWeb"'
				
			}catch(Exception e){
			
				echo "API service is not running"
			}
			
			echo "********************Archiving Old Code Started********************"
			
			sh 'ssh svcdevops_admin@10.1.20.75 "7za.exe a C:\\Softwares\\GxCaptureDeployment\\Backup\\WEB\\GxCaptureWeb_${BUILD_NUMBER}_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip C:\\Softwares\\GxCaptureDeployment\\GxCaptureWeb"'
			
			echo "********************Archived Old Code********************"
			
			sleep 10
			
			echo "delete old code floders"
			
			sh 'ssh svcdevops_admin@10.1.20.75 "del /F /Q C:\\Softwares\\GxCaptureDeployment\\GxCaptureWeb"'
									
			echo "copying build directory to the server"
			
			sh 'mv build GxCaptureWeb'

			sh '7z a GxCaptureWeb.zip GxCaptureWeb -xr!*.svn'
			
			sh 'scp -r /home/jenkins/workspace/GxCaptureDevWeb/GxCaptureWeb.zip svcdevops_admin@10.1.20.75:C:/Softwares/GxCaptureDeployment/'
			
			sh 'ssh svcdevops_admin@10.1.20.75 "7za.exe x C:\\Softwares\\GxCaptureDeployment\\GxCaptureWeb.zip -oC:\\Softwares\\GxCaptureDeployment\\ -y -r && echo "Starting UI Service" && SC START GxCaptureWeb"'	
			
			mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

			cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

			subject: "Dev DEPLOYMENT FINISHED for GxCaptureWeb- Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

			body: "Please go to ${env.BUILD_URL}.");
                             
		}
	}catch(Exception err)
	{				
		
		
		echo 'DEPLOYING PREVIOUS CODE'
		
		sh 'ssh svcdevops_admin@10.1.20.75 "7za.exe x C:\\Softwares\\GxCaptureDeployment\\Backup\\WEB\\GxCaptureWeb_${BUILD_NUMBER}_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip -oC:\\Softwares\\GxCaptureDeployment\\ -y -r"'
					
		echo "Restarting UI Service"
		
		try{
				echo "stop WEB Service"
				
				sh 'ssh svcdevops_admin@10.1.20.75 "SC STOP GxCaptureWeb"'
				
			}catch(Exception e){
			
				echo "API service is not running"
			}
			
		sleep 10
		
		sh 'ssh svcdevops_admin@10.1.20.75 "SC START GxCaptureWeb"'		
			
		currentBuild.result = 'FAILURE'
		
        mail (to: 'mgmappeti@galaxe.com,brkumar@galaxe.com,hgownolla@galaxe.com,vgovindaraj@galaxe.com,skolla@galaxe.com,shegganaik@galaxe.com,nramanjaneyulu@galaxe.com',

        cc: 'mpattem@galaxe.com,happasaheb@galaxe.com,seramanathan@galaxe.com',

        subject: "Dev DEPLOYMENT failed for GxCaptureWeb - Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",

        body: "Please go to ${env.BUILD_URL}.");
		
		

                                throw err
	}
}