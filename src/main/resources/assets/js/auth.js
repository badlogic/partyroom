app.service("AuthService", ["$http", "ipCookie", function($http, ipCookie) {
	var COOKIE = "session";
	var session = ipCookie(COOKIE);
	var isLoggedIn = session != null;
	var profiles = null;
	
	this.signup = function(signupData, keepLogin) {
		return $http.post("app/users/signup", signupData)
		.success(function(data) {
			session = {};
			session.name = signupData.name;
			session.token = data;
			isLoggedIn = true;			
			if(keepLogin) {
				ipCookie(COOKIE, session, { expires: 365 });
			} else {
				ipCookie(COOKIE, session)
			}
		});
	}
	
	this.login = function(loginData, keepLogin) {
		return $http.post("app/users/login", loginData)
		.success(function(data) {
			session = {};
			session.name = loginData.name;
			session.token = data;
			isLoggedIn = true;			
			if(keepLogin) {
				ipCookie(COOKIE, session, { expires: 365 });
			} else {
				ipCookie(COOKIE, session)
			}
		});		
	};
	
	this.logout = function() {
		session = null;
		isLoggedIn = false;
		ipCookie.remove(COOKIE);
	}
	
	this.getUserName = function() {
		if(session != null) return session.name;
		return "";
	}
	
	this.getToken = function() {
		if(session != null) return session.token;
		return "";
	}
	
	this.loggedIn = function() {
		return isLoggedIn;
	};
	
	if(isLoggedIn) {
		$http.post("app/users/getUser", session.sessionToken)
		.error(function(data) {
			session = null;
			isLoggedIn = false;
			ipCookie.remove(KEY);
		})
	}
}]);

app.controller("NavBarController", ["$scope", "$window", "AuthService", function($scope, $window, AuthService) {
	$scope.authService = AuthService;
	
	$scope.logout = function() {
		AuthService.logout();
		$window.location.href="index.html";
	}
}]);