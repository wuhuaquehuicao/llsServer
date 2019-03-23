## Change Log
Date		 	| Note
--------------| ------------------------
Aug 6, 2018	| First Commit
Aug 7, 2018 	| Added Error code, 4.2, 4.3 and 4.4
Aug 8, 2018	| Modified 4.3
Aug 9, 2018	| Added 4.6 and 4.7
Aug 16, 2018	| Update for device entity
Aug 21, 2018	| Update for search

## Error code
Code		 	| Note
------------- | ------------------------
-101			| USER\_DUPLICATED_NAME
-102		 	| USER\_NAME_ERROR
-103			| USER\_PASSWORD_ERROR
-104			| USER\_NOT_FOUND
-201			| LOCATION\_NOT_FOUND\_LOCATION
-301			| DEVICE\_INVALID_MAC
-302			| DEVICE\_DUPLICATED_MAC
-303			| DEVICE\_NOT_FOUND_DEVICE 
-401			| ADLIST_NOT_FOUND_ADLIST
-402			| ADLIST\_INVALID_ADLIST

## 1. User
### 1.1 Login
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/users		| POST	|

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
POST: http://p1000:8081/api/v1/users/login
```
###### Outside network:
```
POST: https://54.191.207.183/api/v1/users/login
```

##### Post Data:
```
{
	"name":"admin@cmn.com",
  	"password":"123456"
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"id": "u_67890",
		"created_at": 1532678247000,
		"updated_at": null,
		"name": "admin@cmn.com",
		"role": "user"
	}
}
```

## 2. Location
### 2.1 Get locations
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/locations	|GET	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/locations?limit=1&offset=0&clause=`name` Like 'admin'&clause=`layout`='0'&order=created_at
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/locations?limit=1&offset=0&clause=`name` Like 'admin'&clause=`layout`='0'&order=created_at
```


#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"items":[
			{
				"id": "loc_12345",
				"created_at": 1533141632000,
				"updated_at": null,
				"name": "KFC",
				"address": "Room 2713, building B, Fengxing plaza, tianhe Road, guangzhou, China",
				"phone": "",
				"email": "jim@justek.us",
				"contact": "",
				"active_adlist_id": null
			}
		],
		"count": 1,
		"totalcount": 0
	}
}
```

### 2.2 Create location
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/locations	|POST	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
POST: http://p1000:8081/api/v1/locations
```
###### Outside network:
```
POST: https://54.191.207.183/api/v1/locations
```


##### Post Data:
```
{
     "name": "Justek",
     "address": "Room 1103, VT101 building B",
     "phone": "020-84118401",
     "email": "support@justek.us",
     "password": ""
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		id": "loc_2018081373168",
		"created_at": 1534126996000,
		"updated_at": 1534126996000,
		"name": "Justek",
		"address": "Room 1103, VT101 building B",
		"phone": "020-84118401",
		"email": "support@justek.us",
		"contact": null,
		"active_adlist_id": null,
		"devices": 0
	}
}
```

### 2.3 Get location
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/locations/{location_id}	|GET	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/locations/loc_2018081373168
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/locations/loc_2018081373168
```


#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		id": "loc_2018081373168",
		"created_at": 1534126996000,
		"updated_at": 1534126996000,
		"name": "Justek",
		"address": "Room 1103, VT101 building B",
		"phone": "020-84118401",
		"email": "support@justek.us",
		"contact": null,
		"active_adlist_id": null,
		"devices": 0
	}
}
```

### 2.4 Update location
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/locations	| PUT	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
PUT: http://p1000:8081/api/v1/locations
```
###### Outside network:
```
PUT: https://54.191.207.183/api/v1/locations
```

##### Put Data:
```
{
     "name": "Justek",
     "address": "Room 1103, VT101 building B",
     "phone": "020-84118401",
     "email": "support@justek.us",
     "password": ""
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		id": "loc_2018081373168",
		"created_at": 1534126996000,
		"updated_at": 1534126996000,
		"name": "Justek",
		"address": "Room 1103, VT101 building B",
		"phone": "020-84118401",
		"email": "support@justek.us",
		"contact": null,
		"active_adlist_id": null,
		"devices": 0
	}
}
```


## 3. Device
### 3.1 Get devices
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/devices		|GET	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/devices?limit=1&offset=0&clause=`name` Like 'admin'&order=created_at
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/devices?limit=1&offset=0&clause=`name` Like 'admin'&order=created_at
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"items":[
			{	
				"id": "AC3743A7EC25",
				"created_at": 1533625655000,
				"updated_at": 1534321930000,
				"name": "Tony's phone(Third floor)",
				"location_id": "loc_67890",
				"password": null,
				"version": "1.0",
				"battery_health": null,
				"battery_level": 70,
				"pause_value": 0,
				"active": 0,
				"last_active_time": null,
				"location_name": "MC"
			}
		],
		"count": 1,
		"totalcount": 0
	}
}
```
### 3.2 Get device
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/devices/{devices_id}	| GET	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/devices/AC3743A7EC25
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/devices/AC3743A7EC25
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		id": "AC3743A7EC25",
		"created_at": 1533625655000,
		"updated_at": 1534321930000,
		"name": "Tony's phone(Third floor)",
		"location_id": "loc_67890",
		"password": null,
		"version": "1.0",
		"battery_health": null,
		"battery_level": 70,
		"pause_value": 0,
		"active": 0,
		"last_active_time": null,
		"location_name": "MC"
	}
}
```


### 3.3 Create device
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/devices		|POST	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
POST: http://p1000:8081/api/v1/devices
```
###### Outside network:
```
POST: https://54.191.207.183/api/v1/devices
```


##### Post Data:
```
{
     "name":"Tony's phone",
     "password":"123456",
     "mac":"AC3743A7EC25"
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"items":[
			{
				id": "AC3743A7EC25",
				"created_at": 1533625655000,
				"updated_at": 1534321930000,
				"name": "Tony's phone(Third floor)",
				"location_id": "loc_67890",
				"password": null,
				"version": "1.0",
				"battery_health": null,
				"battery_level": 70,
				"pause_value": 0,
				"active": 0,
				"last_active_time": null,
				"location_name": "MC"
			}
		],
		"count": 1,
		"totalcount": 0
	}
}
```

### 3.4 Update devices

#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/devices/{device_id}	| PUT	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
PUT: http://p1000:8081/api/v1/devices/ac3743a7ec25
```
###### Outside network:
```
PUT: https://54.191.207.183/api/v1/devices/ac3743a7ec25
```
##### Put Data:
```
{
     "name":"Tony's phone",
     "password":"123456",
     "mac":"AC3743A7EC25",
     "location_id":"",		 
     "battery_level":80, 		// 80%
     "battery_health":"good", // good, overheat, overvoltage, dead
     "version":"1.0.0.1" 
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"items":[
			{
				"id": "AC3743A7EC25",
				"created_at": 1533538523000,
				"updated_at": 1533538523000,
				"name": "Tony's phone",
				"location_id": null,
				"password": null,
				"version": null,
				"battery_health": "good",
				"battery_level": 80,
				"pause_value": 0,
				"active": 0,
				"last_active_time": null
			}
		],
		"count": 1,
		"totalcount": 0
	}
}
```


## 4. Adlist
### 4.1 Get adlists
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists 	| GET	| 

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/adlists?limit=1&offset=0&clause=`name` Like 'admin'&clause=`layout`='0'&order=created_at
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/adlists?limit=1&offset=0&clause=`name` Like 'admin'&clause=`layout`='0'&order=created_at
```

#### Response Value:

##### Sample Code
```
{
	"status": "OK",
	"result":{
		"items":[
			{
				"id": "adl_ab0dd6",
				"created_at": 1532675685000,
				"updated_at": null,
				"location_id": null,
				"password": null,
				"version": null,
				"name": "admin@ads.com",
				"description": "",
				"layout": 0,
				"ads":[
					{
						"id": "ad_c9dc02",
						"created_at": 1532763139000,
						"updated_at": null,
						"adlist_id": "adl_ab0dd6",
						"name": "5b944e30923811e8a3d81f7175ec77d2.jpg",
						"path": "https://d1afxhl1xb1w8i.cloudfront.net/adv_6eac06/5b944e30923811e8a3d81f7175ec77d2.jpg",
						"media_type": "image"
					}
				]
			}
		],
		"count": 1,
		"totalcount": 0
	}
}
```
### 4.2 Get adlist
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists/{adlist_id}		| GET	|

##### Sample Code
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/adlists/adl_2018080736703
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/adlists/adl_2018080736703 
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"id": "adl_2018080736703",
		"created_at": 1533614935000,
		"updated_at": 1533614935000,
		"location_id": "loc_12345",
		"name": "test adlist",
		"description": "test des",
		"layout": 0,
		"ads":[
			{
				"id": "ad_2018080766159",
				"created_at": 1533615125000,
				"updated_at": null,
				"adlist_id": "adl_2018080736703",
				"name": "test1",
				"path": "path1",
				"media_type": "image"
			}
		]
	}
}
```

### 4.3 Get active adlist
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists/active	| GET	|

##### Sample Code
###### Internal network (p1000 - the container name for the api service):
```
GET: http://p1000:8081/api/v1/adlists/active?deviceid=AC3743A7EC25&version=1.0.0&batterystatus=full&batteryhealth=good&batterylevel=75
```
###### Outside network:
```
GET: https://54.191.207.183/api/v1/adlists/active?deviceid=AC3743A7EC25&version=1.0.0&batterystatus=full&batteryhealth=good&batterylevel=75 
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"id": "adl_2018080736703",
		"created_at": 1533614935000,
		"updated_at": 1533614935000,
		"location_id": "loc_12345",
		"name": "test adlist",
		"description": "test des",
		"layout": 0,
		"ads":[
			{
				"id": "ad_2018080766159",
				"created_at": 1533615125000,
				"updated_at": null,
				"adlist_id": "adl_2018080736703",
				"name": "test1.jpg",
				"path": "path1",
				"media_type": "image"
			}
		]
	}
}
```


### 4.4 Create adlist
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists		| POST	|

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
POST: http://p1000:8081/api/v1/adlists
```
###### Outside network:
```
POST: https://54.191.207.183/api/v1/adlists 
```
##### Post Data:
```
{
	"name": "test adlist",
  	"layout": 0,
  	"location_id":"loc_12345",
  	"description":"test des"
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"id": "adl_2018080736703",
		"created_at": 1533614935000,
		"updated_at": 1533614935000,
		"location_id": "loc_12345",
		"name": "test adlist",
		"description": "test des",
		"layout": 0,
		"ads":[]
	}
}
```

### 4.5 Update adlist
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists/{adlist_id}		| PUT	|

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
PUT: http://p1000:8081/api/v1/adlists/adl_2018080736703
```
###### Outside network:
```
PUT: https://54.191.207.183/api/v1/adlists/ adl_2018080736703
```
##### Post Data:
```
{
  	"description":"test adlist"
}
```

#### Response Value:

##### Sample Code:
```
{
	status": "OK",
	"result":{
		"id": "adl_2018080736703",
		"created_at": 1533614935000,
		"updated_at": 1534140294000,
		"name": "test adlist",
		"description": "test adlist",
		"layout": 0,
		"ads":[
			{
				"id": "ad_2018080766159",
				"created_at": 1533615125000,
				"updated_at": null,
				"adlist_id": "adl_2018080736703",
				"name": "test1",
				"path": "path1",
				"media_type": "image"
			},
			{"id": "ad_2018080859691", "created_at": 1533718495000, "updated_at": 1533782309000, "adlist_id": "adl_2018080736703",…}
		]
	}
}
```

### 4.6 Create ads
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists/{adlist_id}/ads		|POST|

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
POST: http://p1000:8081/api/v1/adlists/adl_2018080736703/ads
```
###### Outside network:
```
POST: https://54.191.207.183/api/v1/adlists/adl_2018080736703/ads
```

##### Post Data:
```
{
  "ads":[
    {
      "name":"test1",
      "path":"path1",
      "media_type":"image",
      "label":""
    }
  ]
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"id": "adl_2018080736703",
		"created_at": 1533614935000,
		"updated_at": 1533614935000,
		"location_id": "loc_12345",
		"name": "test adlist",
		"description": "test des",
		"layout": 0,
		"ads":[
			{
				"id": "ad_2018080766159",
				"created_at": 1533615125000,
				"updated_at": null,
				"adlist_id": "adl_2018080736703",
				"name": "test1",
				"path": "path1",
				"media_type": "image"
			}
		]
	}
}
```

### 4.7 Update ad
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists/{adlist_id}/ads/{ad_id}		|PUT|

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
PUT: http://p1000:8081/api/v1/adlists/adl_2018080736703/ads/ad_2018080859691
```
###### Outside network:
```
PUT: https://54.191.207.183/api/v1/adlists/adl_2018080736703/ads/ad_2018080859691
```

##### Post Data:
```
{
	"name":"test1",
	"path":"path1",
	"media_type":"image"
}
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result":{
		"id": "ad_2018080766159",
		"created_at": 1533615125000,
		"updated_at": null,
		"adlist_id": "adl_2018080736703",
		"name": "test1",
		"path": "path1",
		"media_type": "image"
	}
}
```

### 4.8 Delete ad
#### Request:
Name		    	  		|Method| Description
-----------------------|------|------------------------
/api/v{ver}/adlists/{adlist_id}/ads/{ad_id}		|DELETE|

##### Sample Code:
###### Internal network (p1000 - the container name for the api service):
```
DELETE: http://p1000:8081/api/v1/adlists/adl_2018080736703/ads/ad_2018080859691
```
###### Outside network:
```
DELETE: https://54.191.207.183/api/v1/adlists/adl_2018080736703/ads/ad_2018080859691
```

#### Response Value:

##### Sample Code:
```
{
	"status": "OK",
	"result": null
}
```
