function getUrlParameter(sParam) {
	var sPageURL = window.location.search.substring(1);
	var sURLVariables = sPageURL.split('&');
	for (var i = 0; i < sURLVariables.length; i++) {
		var sParameterName = sURLVariables[i].split('=');
		if (sParameterName[0] == sParam) {
			return sParameterName[1];
		}
	}	
}

var player = null;
var params = { allowScriptAccess: "always" };
var atts = { id: "myytplayer" };
swfobject.embedSWF("http://www.youtube.com/apiplayer?enablejsapi=1&playerapiid=ytplayer&version=3",
                   "ytapiplayer", "640", "480", "8", null, null, params, atts);

function onYouTubePlayerReady(id) {
	player = document.getElementById("myytplayer");
}

app.controller("RoomController", ["$scope", "$http", "$location", "$window", "$timeout", "ipCookie", "AuthService", function($scope, $http, $location, $window, $timeout, ipCookie, AuthService) {
	if(!AuthService.loggedIn()) {
		$window.location.href="index.html"
		return;
	};
	
	$scope.playList = [];
	$scope.roomName = getUrlParameter("name");
	$scope.searchResults = [];
	$scope.room = { currentSong: { id: null, user: null }, startTime: 0 };
	
	if(!$scope.roomName) {
		$window.location.href="index.html";
		return;
	};
	
	$scope.sendMessage = function() {
		$http.post("app/rooms/message", { userId: AuthService.getToken(), roomName: $scope.roomName, message: $scope.message}).
		success(function(data) {
			$scope.room = data;
			$timeout(function() { document.getElementById("chatlist").scrollTop = 99999999; }, 0);
		});
		$scope.message = "";
	};
	
	$scope.update = function(timeout) {
		$timeout(function() {
			$http.post("app/rooms/update", { "roomName": $scope.roomName, "userId": AuthService.getToken() }).
			success(function(data) {
				if(player) {
					// we may get a null song, create an empty one in that case
					if(!data.currentSong) data.currentSong = { youtubeId: null, user: null };								
					
					// if the song changed
					if(data.currentSong.youtubeId !== $scope.room.currentSong.youtubeId) {
						// playback the new video
						if(data.currentSong.youtubeId) {							
							$scope.playVideo(data.currentSong.youtubeId, data.startTime);
						} else {
							player.stopVideo();
						}
						
						// update the playlist if the currently playing song is ours
						if($scope.playList.length > 0 &&
						   $scope.playList[0].id === data.currentSong.youtubeId &&
						   $scope.playList[0].user === data.currentSong.user) {
							$scope.playList.shift();
							$scope.updateSong();
						}
					}
					
					$scope.room = data;			
					$timeout(function() { document.getElementById("chatlist").scrollTop = 99999999; }, 0);				
				}
			}).error(function() {
				$window.location.href="index.html";
			});
			$scope.update(2000);
		}, timeout);
	}
	
	$scope.updateSong = function() {
		if($scope.playList.length == 0) {
			$http.post("app/rooms/song", { "userId": AuthService.getToken(), "song": null });
		} else {
			var song = $scope.playList[0];
			$http.post("app/rooms/song", { "userId": AuthService.getToken(), 
										   "song": { "user": AuthService.getUserName(), "duration": song.duration, "youtubeId": song.id, "thumbnail": song.thumbnail, "title": song.title}});
		}
	}
	
	$scope.addSong = function(song) {
		if($.inArray(song, $scope.playList) != -1) return;		
		$scope.playList.push(song);
		$scope.updateSong();
	}
	
	$scope.search = function () {
	      $http.get('https://www.googleapis.com/youtube/v3/search', {
	        params: {
	          key: $scope.room.youtubeKey,
	          type: 'video',
	          maxResults: '10',
	          part: 'id,snippet',
	          fields: 'items/id,items/snippet/title,items/snippet/description,items/snippet/thumbnails/default,items/snippet/channelTitle',
	          q: $scope.query
	        }
	      })
	      .success( function (data) {
    	    var results = [];
    	    var ids = "";
    	    for (var i = data.items.length - 1; i >= 0; i--) {
    	    	var result = {
    	    	    user: AuthService.getUserName(),
    	    		id: data.items[i].id.videoId,
	    	        title: data.items[i].snippet.title,
	    	        description: data.items[i].snippet.description,
	    	        thumbnail: data.items[i].snippet.thumbnails.default.url,
	    	        author: data.items[i].snippet.channelTitle
    	    	};
    	    	results.push(result);
    	    	ids += result.id + ",";
    	    }
    	    $http.get("https://www.googleapis.com/youtube/v3/videos", {
    	    	params: {
    	    		key: $scope.room.youtubeKey,
    	    		id: ids,
    	    		part: "contentDetails"
    	    	}
    	    }).success(function(data) {
    	    	for(var i = 0; i < results.length; i++) {
    	    		results[i].duration = nezasa.iso8601.Period.parseToTotalSeconds(data.items[i].contentDetails.duration);
    	    	}
    	    	$scope.searchResults = results;
    			$timeout(function() { document.getElementById("chatlist").scrollTop = 0; }, 0);
    	    });
	      })  
	}
	
	$scope.isPlaying = function() {
		return player.getPlayerState() >= 1 && player.getPlayerState() <= 3;
	}
	
	$scope.playVideo = function(id, startTime) {
		var offset = (new Date().getTime() - new Date(startTime).getTime()) / 1000
		console.log("playing video " + id + ", " + offset);
		player.loadVideoById(id, offset);		
	}
	
	$scope.getOffset = function() {
		if(!$scope.room.currentSong.youtubeId) return 0;
		return Math.floor((new Date().getTime() - new Date($scope.room.startTime).getTime()) / 1000);
	}
	
	$scope.update(0);
}]);