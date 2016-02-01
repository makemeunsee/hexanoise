function appMain() {

    var scramblMain = demo.webapp.ScramblMain().init();

    // help dialog
    $(function() {
        $( "#dialog" ).dialog({
            autoOpen: false,
            width: "100%",
            height: 100
        });
    });

    var uiVisible = true;
    function toggleUI( show ) {
        if ( show === undefined ) {
            show = !uiVisible;
        }
        if ( show ) {
            $( ".gui" ).show();
            $( ".dg" ).show();
            stats.domElement.style.display = "block";
        } else {
            $( ".gui" ).hide();
            $( ".dg" ).hide();
            stats.domElement.style.display = "none";
        }
        uiVisible = show;
    }

    // jqueryui widgets
    $(function() {
      $( "button" ).button();
    });

    function showShareDialog() {
        if ( $( "#dialog" ).dialog( "isOpen" ) ) {
            $( "#dialog" ).dialog( "close" );
        } else {
            var shareLink = scramblMain.shareLink();
            $( "#dialog" ).append( "<a id='shareLink' href='" + shareLink + "'>" + shareLink + "</a>");
            $( "#dialog" ).dialog( "open" );
        }
    }
    $("#share").unbind("click");
    $("#share").click(showShareDialog);

    $("#reset").unbind("click");
    $("#reset").click(function() {
        window.location = window.location.pathname;
    });

    var takeScreenshot = false;
    function screenshot() {
        takeScreenshot = true;
    }
    $("#screenshot").unbind("click");
    $("#screenshot").click(screenshot);

    $("#nextflake").unbind("click");
    $("#nextflake").click(function() { scramblMain.next(); });

    var toggleFullscreen = THREEx.FullScreen.toggleFct();
    $("#fullscreen").unbind("click");
    $("#fullscreen").click(toggleFullscreen);

    var mainContainer = $( "#main" )[0];

    var updateProjection = function(screenWidth, screenHeight) {
        scramblMain.scene.updateViewport( screenWidth, screenHeight );
    };

    function leftButton(evt) {
        var button = evt.which || evt.button;
        return button == 1;
    }

    var mx, my = 0;

    var tapped = false; // double tap detection

    function onMouseDown(event) {
        if (leftButton(event)) {
            event.touches = [{clientX: event.clientX, clientY: event.clientY}];
            onTouchStart(event);
            clearTimeout(tapped);
            tapped = null; // double click is handled natively
        }
    }

    var clicked = false;
    var dragging = false;

    // only react to left clicks
    function onTouchStart(event) {
        // dont handle multi touch
        if (event.touches.length === 1) {
            mx = event.touches[0].clientX;
            my = event.touches[0].clientY;
            canvas.addEventListener( "mouseup", onMouseUp, false );
            canvas.addEventListener( "touchend", onTouchEnd, false );
            canvas.addEventListener( "mousemove", onMouseMove, false );
            canvas.addEventListener( "touchmove", onTouchMove, false );
            if(!tapped){ //if tap is not set, set up single tap
                tapped = setTimeout(function() {
                    tapped = null
                }, 300);   //wait 300ms then run single click code
            } else {    //tapped within 300ms of last tap. double tap
              clearTimeout(tapped); //stop single tap callback
              tapped = null;
              doubleClick();
            }
        }
    }

    function doubleClick() {
        toggleUI();
    }

    function onMouseUp(event) {
        if (leftButton(event)) {
            onTouchEnd(event);
        }
    }

    function onTouchEnd(event) {
        canvas.removeEventListener( "mouseup", onMouseUp, false );
        canvas.removeEventListener( "touchend", onTouchEnd, false );
        canvas.removeEventListener( "mousemove", onMouseMove, false );
        canvas.removeEventListener( "touchmove", onTouchMove, false );
        clicked = !dragging;
        dragging = false;
    }

    function onMouseMove(event) {
        event.touches = [{clientX: event.clientX, clientY: event.clientY}];
        onTouchMove(event);
    }

    // mouse drag -> move camera (adjusted to zoom)
    function onTouchMove(event) {
        // dont handle multi touch
        if (event.touches.length === 1) {
            dragging = true;
            event.preventDefault();
            var deltaX = event.touches[0].clientX - mx;
            var deltaY = event.touches[0].clientY - my;

            scramblMain.scene.dragView( deltaX, deltaY );

            mx = event.touches[0].clientX;
            my = event.touches[0].clientY;
            // no need to update cam, projection matrix is not changed by translation
        }
    }

    // mouse wheel -> zoom in / out
    function onMouseWheel(event) {
        scramblMain.scene.zoom( Math.max( -1, Math.min( 1, ( event.wheelDelta || -event.detail ) ) ) );
    }

    // pinch detection (and more)
    var mc = new Hammer(mainContainer);
    mc.get("pinch").set({ enable: true });

    mc.on("pinch", function(ev) {
        scramblMain.scene.zoom(ev.scale < 1 ? -1 : 1);
    });

    var renderer = scramblMain.scene.renderer;
    var canvas = renderer.domElement;

    canvas.addEventListener( "mousedown", onMouseDown, false );
    canvas.addEventListener( "touchstart", onTouchStart, false );

    canvas.addEventListener( "mousewheel", onMouseWheel, false );
    // Firefox
    canvas.addEventListener( "DOMMouseScroll", onMouseWheel, false );

    canvas.addEventListener( "dblclick", doubleClick, false );

    THREEx.WindowResize(renderer, updateProjection);
    THREEx.FullScreen.bindKey({ charCode : 'f'.charCodeAt(0) });

    // A cross-browser requestAnimationFrame
    // See https://hacks.mozilla.org/2011/08/animating-with-javascript-from-setinterval-to-requestanimationframe/
    var requestAnimFrame = (function() {
        return window.requestAnimationFrame    ||
            window.webkitRequestAnimationFrame ||
            window.mozRequestAnimationFrame    ||
            window.oRequestAnimationFrame      ||
            window.msRequestAnimationFrame     ||
            function(callback){
                window.setTimeout(callback, 1000 / 60);
            };
    })();

    // Don't run the game when the tab isn't visible
    window.addEventListener('focus', function() {
        unpause();
    });

    window.addEventListener('blur', function() {
        pause();
    });

    var running = true;

    // Pause and unpause
    function pause() {
        running = false;
    }

    function unpause() {
        running = true;
        main();
    }

    // canvas & webgl context code

    updateProjection(window.innerWidth, window.innerHeight);
    renderer.setSize( window.innerWidth, window.innerHeight );
    mainContainer.appendChild( canvas );

    var stats = new Stats();
    stats.setMode( 1 ); // 0: fps, 1: ms, 2: mb

    stats.domElement.style.position = 'absolute';
    stats.domElement.style.left = '0px';
    stats.domElement.style.bottom = '0px';

    document.body.appendChild( stats.domElement );

    scramblMain.loadModel(toggleUI, requestAnimFrame, main);

    // The main game loop
    function main() {
        if(!running) {
            return;
        }

        stats.begin();

        if (takeScreenshot) {

            scramblMain.scene.renderNoTexture();
            canvas.toBlob(function(blob) {
                saveAs(blob, "screenshot.png");
            });

            takeScreenshot = false;
        }

        // normal render
        scramblMain.scene.render();

        stats.end();

        requestAnimFrame(main);
    }


};

window.onload = appMain;