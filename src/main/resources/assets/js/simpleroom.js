app.controller("SimpleRoomController", ["$scope", "$http", "$location", "$window", "$timeout", "$modal", "ipCookie", "AuthService",
                          function($scope, $http, $location, $window, $timeout, $modal, ipCookie, AuthService) {
  $scope.playList = [];
  $scope.roomName = getUrlParameter("name");
  $scope.searchResults = [];
  $scope.room = { users: [], currentSong: { id: null, user: null }, startTime: 0, playedTime: 0 };

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
    if(!data.currentSong) data.currentSong = { url: null, user: null };
    if(!$scope.room.currentSong) $scope.room.currentSong = { url: null, user: null };

    // if the song changed
    if(data.currentSong.url !== $scope.room.currentSong.url) {
      // update the playlist if the currently playing song is ours
      if($scope.playList.length > 0 &&
         $scope.playList[0].url === data.currentSong.url &&
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
      }).error(function() {
        $window.location.href="index.html";
      });
      $scope.update(2000);
    }, timeout);
  }

  $scope.getOffset = function() {
    if(!$scope.room.currentSong || (!$scope.room.currentSong.url)) return 0;
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
                "url": result.id,
                "thumbnail": result.thumbnail,
                "title": result.title
              };
              songs.push(song);
            }
            $scope.searchResults = songs;
          });
        })
  }

  $scope.updateSong = function() {
    $http.post("app/rooms/song", { "userId": AuthService.getToken(), "roomName": $scope.roomName, "playList": $scope.playList });
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
