var player = null;
var tag = document.createElement('script');
tag.src = "https://www.youtube.com/iframe_api";
var firstScriptTag = document.getElementsByTagName('script')[0];
firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

function onYouTubeIframeAPIReady() {
	new YT.Player('player', {
        height: '480',
        width: '640',
        videoId: 'cdwal5Kw3Fc',
        events: {
          'onReady': onPlayerReady,
        }
      });
}

function onPlayerReady(event) {
	player = event.target;
}

app.controller("RoomController", ["$scope", "$http", "$location", "$window", "$timeout", "$modal", "ipCookie", "AuthService",
                          function($scope, $http, $location, $window, $timeout, $modal, ipCookie, AuthService) {
	$scope.playList = [];
	$scope.roomName = getUrlParameter("name");
	$scope.searchResults = [];
	$scope.room = { users: [], currentSong: { id: null, user: null }, startTime: 0, playedTime: 0 };
	$scope.playlists = [];
	$scope.selectedPlaylist = null;

	// scroll to bottom for login/join flow

	$scope.join = function() {
		// join the room and start the heartbeat
		$http.post("app/rooms/join", { "userId": AuthService.getToken(), "roomName": $scope.roomName }).
		success(function(data) {
			$scope.processUpdate(data);
			$scope.update(0);
		}).error(function() {
			AuthService.logout();
			$window.location.href="index.html";
		});
	}

	$scope.processUpdate = function(data) {
		// we may get a null song, create an empty one in that case
		if(!data.currentSong) data.currentSong = { youtubeId: null, user: null };
		if(!$scope.room.currentSong) $scope.room.currentSong = { youtubeId: null, user: null };

		// if the song changed
		if(data.currentSong.youtubeId !== $scope.room.currentSong.youtubeId) {
			// playback the new video
			if(data.currentSong.youtubeId) {
				$scope.playVideo(data.currentSong.youtubeId, data.playedTime, data.currentSong.duration);
			} else {
				if(player) player.stopVideo();
			}

			// update the playlist if the currently playing song is ours
			if($scope.playList.length > 0 &&
			   $scope.playList[0].youtubeId === data.currentSong.youtubeId &&
			   $scope.playList[0].user === data.currentSong.user) {
				$scope.playList.shift();
				$scope.updateSong();
			}
		}
		$scope.room = data;
	}

	$scope.update = function(timeout) {
		$timeout(function() {
			$http.post("app/rooms/update", { "userId": AuthService.getToken(), "roomName": $scope.roomName }).
			success(function(data) {
				$scope.processUpdate(data)
				$timeout(function() { document.getElementById("chatlist").scrollTop = 99999999; }, 0);
			}).error(function() {
				$window.location.href="index.html";
			});
			$scope.update(2000);
		}, timeout);
	}

	$scope.sendMessage = function() {
		$http.post("app/rooms/message", { "userId": AuthService.getToken(), "roomName": $scope.roomName, "message": $scope.message}).
		success(function(data) {
			$scope.room = data;
			$timeout(function() { document.getElementById("chatlist").scrollTop = 99999999; }, 0);
		});
		$scope.message = "";
	};

	$scope.isPlaying = function() {
		if(!player) return false;
		return player.getPlayerState() >= 1 && player.getPlayerState() <= 3;
	}

	$scope.playVideo = function(id, startTime, duration) {
		if(!player) return;
		if(startTime < 0) startTime = 0;
		if(startTime > duration) startTime = duration;
		console.log("playing video " + id + ", " + startTime);
		player.loadVideoById(id, startTime);
	}

	$scope.getOffset = function() {
		if(!$scope.room.currentSong || !$scope.room.currentSong.youtubeId) return 0;
		return $scope.room.playedTime;
	}

	$scope.currentSong = function() {
		return $scope.room.currentSong;
	}

	$scope.currentUser = function() {
		if($scope.room.users.length == 0) return null;
		return $scope.room.users[$scope.room.currentUser].name;
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
	  	    	var songs = [];
						for(var i = 0; i < data.items.length; i++) {
							var result = results[i];
							var song = {
								"user": AuthService.getUserName(),
								"duration": nezasa.iso8601.Period.parseToTotalSeconds(data.items[i].contentDetails.duration),
								"youtubeId": result.id,
								"thumbnail": result.thumbnail,
								"title": result.title
							};
							songs.push(song);
						}
						$scope.searchResults = songs;
	  				$timeout(function() { document.getElementById("chatlist").scrollTop = 0; }, 0);
	  	    });
	      })
	}

	$scope.updateSong = function() {
		if($scope.playList.length == 0) {
			$http.post("app/rooms/song", { "userId": AuthService.getToken(), "roomName": $scope.roomName, "song": null });
		} else {
			var song = $scope.playList[0];
			$http.post("app/rooms/song", { "userId": AuthService.getToken(), "roomName": $scope.roomName, "song": song});
		}
	}

	$scope.addSong = function(song) {
		$scope.isSearching = false;
		if($.inArray(song, $scope.playList) != -1) return;
		$scope.playList.push(song);
		$scope.updateSong();
	}

	$scope.removeSong = function(song) {
		var idx = $scope.playList.indexOf(song);
		if(idx > -1) {
			$scope.playList.splice(idx, 1);
		}
		$scope.updateSong();
	}

	$scope.moveSongUp = function(song) {
		var idx = $scope.playList.indexOf(song);
		if(idx > 0) {
			$scope.playList.move(idx, idx - 1);
		}
		$scope.updateSong();
	}

	$scope.moveSongDown = function(song) {
		var idx = $scope.playList.indexOf(song);
		if(idx >= 0 && idx < $scope.playList.length - 1) {
			$scope.playList.move(idx, idx + 1);
		}
		$scope.updateSong();
	}

	$scope.shuffleSongs = function() {
		shuffle($scope.playList);
		$scope.updateSong();
	}

	$scope.upvote = function() {
		$http.post("app/rooms/vote", {
			"userId": AuthService.getToken(),
			"roomName": $scope.roomName,
			"vote": 1
		});
	}

	$scope.downvote = function() {
		$http.post("app/rooms/vote", {
			"userId": AuthService.getToken(),
			"roomName": $scope.roomName,
			"vote": -1
		});
	}

	$scope.getPlaylists = function() {
		$http.post("app/users/getPlaylists", AuthService.getToken() )
		.success(function(data) {
			$scope.playlists = data.playlists;
			$scope.selectedPlaylist = null;
		}).error(function() {
			$scope.playlists = [];
			$scope.selectedPlaylist = null;
		});
	}

	$scope.selectPlaylist = function(playlist) {
		$scope.selectedPlaylist = playlist;
	}

	$scope.addPlaylist = function(playlist) {
		$scope.isSearching = false;
		for(var i = 0; i < playlist.items.length; i++) {
			var song = playlist.items[i];
			if($.inArray(song, $scope.playList) != -1) continue;
			$scope.playList.push(song);
		}
		$scope.updateSong();
	}

	if(!$scope.roomName) {
		$window.location.href="index.html";
		return;
	} else {
		$scope.roomName = decodeURIComponent($scope.roomName);
	}

	// we aren't signed in, ask for sign in/up before joining
	if(!AuthService.loggedIn()) {
		AuthService.showLoginDialog(function() {
			$scope.join();
		}, function() {
			$scope.join();
		}, function() {
			$window.location.href = "index.html";
		});
	} else {
		$scope.join();
	}
}]);
