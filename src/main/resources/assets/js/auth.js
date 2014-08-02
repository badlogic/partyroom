app.service("AuthService", ["$http", "$modal", "ipCookie", function($http, $modal, ipCookie) {
	var COOKIE = "session";
	var session = ipCookie(COOKIE);
	var isLoggedIn = session != null;
	var profiles = null;
	var self = this;
	
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
	
	this.showLoginDialog = function(loginCallback, signupCallback, cancelCallback) {		
		var modalInstance = $modal.open({
			template: 
				'<div class="modal-header">' +
				'<h3 class="modal-title">Login or Sign up!</h3>' +
				'</div>' +
				'<div class="modal-body">' +
					'<form class="form-signin" role="form">' +
						'<div ng-show="error" class="alert alert-danger">{{errorMessage}}</div>' +
						'<input type="text" class="form-control" placeholder="User name" required autofocus ng-model="loginData.name">' +
						'<input type="password" class="form-control" placeholder="Password" required ng-model="loginData.password">' +
						'<label class="checkbox"> <input type="checkbox" value="remember-me" ng-model="keepLogin">Remember me</label>' +
						'<button class="btn btn-lg btn-primary btn-block" type="submit" ng-click="login()">Login</button>' +
						'<button class="btn btn-lg btn-primary btn-block" type="submit" ng-click="signup()">Sign up</button>' +
					'</form>' +
				'</div>' +
				'<div class="modal-footer">' +            
					'<button class="btn btn-warning" ng-click="cancel()">Cancel</button>' +
				'</div>',
			controller: function ($scope, $modalInstance) {
				$scope.loginData = { name: "", password: "" };
				
				$scope.error = false;
				$scope.keepLogin = false;
				$scope.loginData = {};
				
				$scope.signup = function() {
					if(!$scope.loginData.name || !$scope.loginData.password) return;
					self.signup($scope.loginData, $scope.keepLogin).success(function(data) {
						$modalInstance.dismiss('cancel');
						if(signupCallback) signupCallback();
					})
					.error(function(data) {
						$scope.error = true;
						$scope.errorMessage = "Username already taken";
					});
				};
				
				$scope.login = function() {
					if(!$scope.loginData.name || !$scope.loginData.password) return;
					self.login($scope.loginData, $scope.keepLogin).success(function(data) {
						$modalInstance.dismiss('cancel');
						if(loginCallback) loginCallback();
					})
					.error(function(data) {
						$scope.error = true;			
						$scope.errorMessage = "Username or password are wrong";
					});
				};
				
				$scope.cancel = function() {
					$modalInstance.dismiss('cancel');
					if(cancelCallback) cancelCallback();
				}
			},
			keyboard: false
		});			
	}
	
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
	
	$scope.login = function() {
		AuthService.showLoginDialog(function() {
		}, function() {
		}, function() {
			$window.location.href = "index.html";
		});
	}
	
	$scope.logout = function() {
		AuthService.logout();
		$window.location.href="index.html";
	}
}]);