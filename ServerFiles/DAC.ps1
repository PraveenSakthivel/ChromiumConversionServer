Get-Content C:/Users/pearson2/Documents/vars.txt | Foreach-Object{
   $var = $_.Split('=')
   New-Variable -Name $var[0] -Value $var[1]
}
$SourcePath = $($BuildPath + "\nwjs")
cd "C:\Program Files\WindowsApps\Microsoft.DesktopAppConverter_2.0.2.0_x64__8wekyb3d8bbwe"
DesktopAppConverter
set-ExecutionPolicy bypass
Start-Transcript -path C:\Users\pearson2\documents\powershellOutput.txt -append
DesktopAppConverter.exe -Installer $SourcePath -AppExecutable nw.exe -Destination $BuildPath -PackageName "RealizeReader" -Publisher "CN=Pearson" -Version $Version -MakeAppx -Sign -Verbose
stop-process -Id $PID