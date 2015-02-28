#!/system/bin/sh

send_commands()
{
	cat /dev/ttyGS0 &
	CAT_PROCESS=$!

	#stop ril-daemon
	sleep 1
	echo -e 'ATE1\r' > /dev/ttyGS0
	sleep 1
	echo -e 'ATV1\r' > /dev/ttyGS0  
	sleep 1
	echo -e 'AT+CIMI\r' > /dev/ttyGS0  #2.IMSI (SIM & USIM)
	sleep 1
	echo -e 'AT+COPS?\r' > /dev/ttyGS0 #13. Provider (SIM & USIM)
	sleep 1
	echo -e 'AT+CREG=2\r' > /dev/ttyGS0 #14. Set sim card in registration code to get LAC and CellID (SIM & USIM)
	sleep 1
	echo -e 'AT+CREG?\r' > /dev/ttyGS0 #14. LAC (SIM & USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28589,0,0,3\r' > /dev/ttyGS0 #1.Ciphering Mode (SIM & USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28448,0,0,9\r' > /dev/ttyGS0 #3. Kc SIM
	sleep 1
	echo -e 'AT+CRSM=176,20256,0,0,9\r' > /dev/ttyGS0 #3. Kc USIM
	sleep 1
	echo -e 'AT+CRSM=176,28498,0,0,9\r' > /dev/ttyGS0 #4. KcGPRS (SIM)
	sleep 1
	echo -e 'AT+CRSM=176,20306,0,0,9\r' > /dev/ttyGS0 #4. KcGPRS (USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28424,0,0,33\r' > /dev/ttyGS0 #5. CK & IK (USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28542,0,0,11\r' > /dev/ttyGS0 #7. TMSI & TMSI TIME & LAI (SIM &USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28499,0,0,14\r' > /dev/ttyGS0 #8. PTMSI & PTMSI signature value & RAI & RAUS (SIM)
	sleep 1
	echo -e 'AT+CRSM=176,28531,0,0,14\r' > /dev/ttyGS0 #8. PTMSI & PTMSI signature value & RAI & RAUS (uSIM)
	sleep 1
	echo -e 'AT+CRSM=176,28508,0,0,3\r' > /dev/ttyGS0 #12. THRESHOLD (USIM)
	sleep 1
 	
	#start ril-daemon
	kill -9 $CAT_PROCESS
}

#-------------- MAIN PROGRAM -----------------------

	#NOW=$(date +"%Y-%m-%d-%H-%M-%S")
	#CURRENT_COM="/data/data/com.dg.simmonitor/tmp/android-$NOW-commands.txt"
	send_commands
	#CURRENT_COM=""
	#echo "$NOW - $c time - successfully"
	
#---------------------------------------------------
