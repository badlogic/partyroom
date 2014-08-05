app.controller("LobbyController", ["$scope", "$http", "$window", "$modal", "ipCookie", "AuthService", function($scope, $http, $window, $modal, ipCookie, AuthService) {
	$scope.playLists = null;
	$scope.currPlaylist = null;
	$scope.youtubeKey = null;

	$http.post("app/users/getPlaylists", AuthService.getToken()).success(function(data) {
		$scope.playLists = data.playlists;
		$scope.youtubeKey = data.youtubeKey;
	}).error(function() {
		AuthService.logout();
		$window.location.href="index.html";
	});

	$scope.playlistClicked = function(playlist) {
		$scope.currPlaylist = playlist;
	}

	$scope.savePlaylist = function(playlist) {
		$http.post("app/users/updatePlaylist", { "userId": AuthService.getToken(), "playlist": playlist });
	}

	$scope.addSong = function(song) {
		$scope.isSearching=false;
		if($.inArray(song, $scope.currPlaylist.items) != -1) return;
		$scope.currPlaylist.items.push(song);
		$scope.savePlaylist($scope.currPlaylist);
	}

	$scope.removeSong = function(song) {
		var idx = $scope.currPlaylist.items.indexOf(song);
		if(idx > -1) {
			$scope.currPlaylist.items.splice(idx, 1);
		}
		$scope.savePlaylist($scope.currPlaylist);
	}

	$scope.moveSongUp = function(song) {
		var idx = $scope.currPlaylist.items.indexOf(song);
		if(idx > 0) {
			$scope.currPlaylist.items.move(idx, idx - 1);
		}
		$scope.savePlaylist($scope.currPlaylist);
	}

	$scope.moveSongDown = function(song) {
		var idx = $scope.currPlaylist.items.indexOf(song);
		if(idx >= 0 && idx < $scope.currPlaylist.items.length - 1) {
			$scope.currPlaylist.items.move(idx, idx + 1);
		}
		$scope.savePlaylist($scope.currPlaylist);
	}

	$scope.shuffleSongs = function() {
		shuffle($scope.currPlaylist.items);
		$scope.savePlaylist($scope.currPlaylist);
	}

	$scope.newPlaylist = function() {
		var createCallback = function(name) {
			var playlist = { "name": name, items: [] };
			$scope.playLists.push(playlist);
			$scope.currPlaylist = playlist;
			$scope.savePlaylist($scope.currPlaylist);
		}

		var outerScope = $scope;

		var modalInstance = $modal.open({
			template:
				'<div class="modal-header">' +
				'<h3 class="modal-title">New Playlist</h3>' +
				'</div>' +
				'<div class="modal-body">' +
					'<form class="form-signin" role="form">' +
						'<div ng-show="errorMessage" class="alert alert-danger">{{errorMessage}}</div>' +
						'<input type="text" class="form-control" placeholder="Playlist name" required autofocus ng-model="name">' +
					'</form>' +
				'</div>' +
				'<div class="modal-footer">' +
					'<button class="btn btn-primary" type="submit" ng-click="create(name)">Create</button>' +
					'<button class="btn btn-warning" ng-click="cancel()">Cancel</button>' +
				'</div>',
			controller: function ($scope, $modalInstance) {
				$scope.create = function(name) {
					if(!name || name.length == 0) {
						$scope.errorMessage = "No name given";
						return;
					}

					for(var i = 0; i < outerScope.playLists.length; i++) {
						var otherPlaylist = outerScope.playLists[i];
						if(otherPlaylist.name === name) {
							$scope.errorMessage = "A playlist with that name already exists";
							return;
						}
					}

					$modalInstance.dismiss('cancel');
					createCallback(name);
				};

				$scope.cancel = function() {
					$modalInstance.dismiss('cancel');
				}
			},
			keyboard: false
		});
	}

	$scope.search = function () {
	      $http.get('https://www.googleapis.com/youtube/v3/search', {
	        params: {
	          key: $scope.youtubeKey,
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
			    		key: $scope.youtubeKey,
			    		id: ids,
			    		part: "contentDetails"
			    	}
			    }).success(function(data) {
			    	var songs = [];
			    	for(var i = 0; i < data.items.length; i++) {
			    		var result = results[i];
							var song = { "user": AuthService.getUserName(),
													 "duration": nezasa.iso8601.Period.parseToTotalSeconds(data.items[i].contentDetails.duration),
													 "youtubeId": result.id,
													 "thumbnail": result.thumbnail,
													 "title": result.title }
			    		songs.push(song);
			    	}
			    	$scope.searchResults = songs;
			    });
	      })
	}

	$scope.searchUrl = function() {
		console.log($scope.query);
	}
}]);
