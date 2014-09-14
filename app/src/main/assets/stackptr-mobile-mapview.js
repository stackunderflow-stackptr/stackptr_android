$(document).ready(function() {
    map = L.map('map-canvas').setView([-34.929, 138.601], 13);
    var tp = "tiles";
    if(L.Browser.retina) {
        tp = "tiles_r";
    };

    L.tileLayer('https://stackptr.com/'+tp+'/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 18
    }).addTo(map);

    updateFollowing();

});