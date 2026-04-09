@echo off
echo Opening TCP and UDP port 5555 in Windows Firewall...

netsh advfirewall firewall add rule ^
    name="Open Port 5555 TCP" ^
    dir=in action=allow ^
    protocol=TCP ^
    localport=5555

netsh advfirewall firewall add rule ^
    name="Open Port 5555 UDP" ^
    dir=in action=allow ^
    protocol=UDP ^
    localport=5555

echo Done.
pause