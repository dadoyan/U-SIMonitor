#!/system/bin/sh
echo mitsos
if[ $1 == s ]; then
	echo stop-ril daemon
else
	echo DO NOT stop-ril daemon
fi
echo $1
