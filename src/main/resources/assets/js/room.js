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

app.controller("RoomController", ["$scope", "$http", "$location", "$window", "$timeout", "ipCookie", "AuthService", function($scope, $http, $location, $window, $timeout, ipCookie, AuthService) {
	if(!AuthService.loggedIn()) {
		$window.location.href="index.html"
		return;
	};
			
	$scope.playList = [];
	$scope.roomName = getUrlParameter("name");
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
				$scope.room = data;
				$timeout(function() { document.getElementById("chatlist").scrollTop = 99999999; }, 0);				
			}).error(function() {
				$window.location.href="index.html";
			});
			$scope.update(2000);
		}, timeout);
	}
	
	$scope.addSong = function(song) {
		$scope.playList.push(song);
	}
	
	$scope.search = function () {
	      $http.get('https://www.googleapis.com/youtube/v3/search', {
	        params: {
	          key: 'AIzaSyDLnw29DUCETqv9gL2YtCx6iLNkEmt1ArE',
	          type: 'video',
	          maxResults: '10',
	          part: 'id,snippet',
	          fields: 'items/id,items/snippet/title,items/snippet/description,items/snippet/thumbnails/default,items/snippet/channelTitle',
	          q: $scope.query
	        }
	      })
	      .success( function (data) {
    	    $scope.searchResults = [];
    	    $timeout(function() { document.getElementById("searchlist").scrollTop = 0; }, 0);
    	    for (var i = data.items.length - 1; i >= 0; i--) {
    	    	$scope.searchResults.push({
    	        id: data.items[i].id.videoId,
    	        title: data.items[i].snippet.title,
    	        description: data.items[i].snippet.description,
    	        thumbnail: data.items[i].snippet.thumbnails.default.url,
    	        author: data.items[i].snippet.channelTitle
    	      });
    	    }    	    	        
	      })  
	}
	
	$scope.update(0);
}]);