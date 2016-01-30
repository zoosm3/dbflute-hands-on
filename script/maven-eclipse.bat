%~d0
cd %~p0
cd ..

:: creating eclipse settings
cmd /c mvn -e eclipse:eclipse

:: refreshing this eclipse project
set pause_at_end=n
call dbflute_maihamadb/manage.bat refresh

pause
