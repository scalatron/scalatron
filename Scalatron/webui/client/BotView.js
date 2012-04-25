BotView = {

    drawToCanvas: function( canvas, state ) {
        if (canvas.getContext) {
            var ctx = canvas.getContext('2d');

            var cellSize = 7;
            var maxViewSize = 31;

            // clear background - black
            ctx.fillStyle = 'rgb(0,0,0)';
            ctx.fillRect (0, 0, maxViewSize * cellSize, maxViewSize * cellSize);

            if(state != null) {

                var size = Math.sqrt(state.length);

                // center the minibot view.
                var offset =  (maxViewSize-size)*cellSize/2;

                // paint emtpy cells for visible playground
                ctx.fillStyle = 'rgb(103,100,100)';
                ctx.fillRect (offset, offset, size * cellSize, size * cellSize);


                for(i=0; i<state.length; i = i+1) {
                    var c = state.charAt(i);

                    if(c != "_") {


                        var y = (i/size)*cellSize +offset ;
                        var x = (i%size)*cellSize +offset;

                        // sorted in order of likelihood
                        if(c == "?") {
                            // Occluded
                            ctx.fillStyle = 'rgb(0,0,0)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "W") {
                            // Wall
                            ctx.fillStyle = 'rgb(55,55,55)';
                            ctx.fillRect (x+1, y+1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(84,80,80)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "P") {
                            // Good plant
                            ctx.fillStyle = 'rgb(0,180,0)';
                            ctx.fillRect (x-1, y-1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(0,120,0)';
                            ctx.fillRect (x+1, y+1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(0,150,0)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "p") {
                            // Bad plant
                            ctx.fillStyle = 'rgb(120,120,0)';
                            ctx.fillRect (x-1, y-1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(180,180,0)';
                            ctx.fillRect (x+1, y+1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(150,150,0)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "B") {
                            // Good beast
                            ctx.fillStyle = 'rgb(0,0,180)';
                            ctx.fillRect (x-1, y-1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(0,0,120)';
                            ctx.fillRect (x+1, y+1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(0,0,150)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "b") {
                            // Bad beast
                            ctx.fillStyle = 'rgb(180,0,0)';
                            ctx.fillRect (x-1, y-1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(120,0,0)';
                            ctx.fillRect (x+1, y+1, cellSize, cellSize);
                            ctx.fillStyle = 'rgb(150,0,0)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "m") {
                            // enemy master
                            ctx.fillStyle = 'rgb(200,0,200)';
                            ctx.beginPath();
                            ctx.arc(x+cellSize/2, y+cellSize/2, cellSize, 0, Math.PI*2, true);
                            ctx.closePath();
                            ctx.fill();
                            ctx.fillStyle = 'rgb(255,0,255)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "s") {
                            // enemy slave
                            ctx.fillStyle = 'rgb(255,0,255)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "S") {
                            // this slave
                            ctx.fillStyle = 'rgb(255,255,255)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        if(c == "M") {
                            // this master
                            ctx.fillStyle = 'rgb(200,200,200)';
                            ctx.beginPath();
                            ctx.arc(x+cellSize/2, y+cellSize/2, cellSize, 0, Math.PI*2, true);
                            ctx.closePath();
                            ctx.fill();
                            ctx.fillStyle = 'rgb(255,255,255)';
                            ctx.fillRect (x, y, cellSize, cellSize);
                        } else
                        {
                            // empty cell
                        }
                    }
                }
            }
        }
    }
};

Events.on("entitySelectionChanged", function() {
    var canvas = Ext.get('botview' ).dom;
    var state = Sandbox.getSelectedEntityView();
    BotView.drawToCanvas(canvas, state);
});