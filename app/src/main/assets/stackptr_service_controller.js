app.controller("StackPtrServiceController", ['$scope', function($scope) {

	// android client:
	$scope.serviceRunning = StackPtrAndroidShim.serviceRunning() == "true";

	$scope.stopService = function() {
		console.log("stopping service")
		StackPtrAndroidShim.serviceStop();
		$scope.serviceRunning = false;
	}
	$scope.startService = function() {
		console.log("starting service")
		StackPtrAndroidShim.serviceStart();
		$scope.serviceRunning = true;
	}

}]);

