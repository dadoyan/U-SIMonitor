#!/system/bin/sh

CURRENT_COM=""
CURRENT_RES=""

send_commands()
{
	#stop ril-daemon
	#cat /dev/smd0 > $CURRENT_COM &
	cat /dev/smd0 &
	CAT_PROCESS=$!

	sleep 1
	echo -e 'ATE1\r' > /dev/smd0  	
	#echo +CIMI
	#sleep 1
	#echo -e 'AT+CIMI\r' > /dev/smd0  #2.IMSI (SIM & USIM)
	#sleep 1
	#echo CIMI_END
	sleep 1
	echo -e 'AT+COPS?\r' > /dev/smd0 #13. Provider (SIM & USIM)
	sleep 1
	echo -e 'AT+CREG=2\r' > /dev/smd0 #14. Set sim card in registration code to get LAC and CellID (SIM & USIM)
	sleep 1
	echo -e 'AT+CREG?\r' > /dev/smd0 #14. LAC (SIM & USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28423,0,0,9\r' > /dev/smd0 #IMSI
	sleep 1
	echo -e 'AT+CRSM=176,28589,0,0,3\r' > /dev/smd0 #1.Ciphering Mode (SIM & USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28448,0,0,9\r' > /dev/smd0 #3. Kc SIM
	sleep 1
	echo -e 'AT+CRSM=176,20256,0,0,9\r' > /dev/smd0 #3. Kc USIM
	sleep 1
	echo -e 'AT+CRSM=176,28498,0,0,9\r' > /dev/smd0 #4. KcGPRS (SIM)
	sleep 1
	echo -e 'AT+CRSM=176,20306,0,0,9\r' > /dev/smd0 #4. KcGPRS (USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28424,0,0,33\r' > /dev/smd0 #5. CK & IK (USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28542,0,0,11\r' > /dev/smd0 #7. TMSI & TMSI TIME & LAI (SIM &USIM)
	sleep 1
	echo -e 'AT+CRSM=176,28499,0,0,14\r' > /dev/smd0 #8. PTMSI & PTMSI signature value & RAI & RAUS (SIM)
	sleep 1
	echo -e 'AT+CRSM=176,28531,0,0,14\r' > /dev/smd0 #8. PTMSI & PTMSI signature value & RAI & RAUS (uSIM)
	sleep 1
	echo -e 'AT+CRSM=176,28508,0,0,3\r' > /dev/smd0 #12. THRESHOLD (USIM)
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
	echo "exit"
#---------------------------------------------------
