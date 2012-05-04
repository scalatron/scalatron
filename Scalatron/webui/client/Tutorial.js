TutorialHelper = {
    // each tutorial HTML document (generated from Markdown) contains a DIV node carrying
    // properties like the prev/next urls
    getTutorialDocumentData: function (attributeName) {
        var prevNext = Ext.get('TutorialDocumentData');
        if(prevNext != null) {
            return prevNext.getAttribute(attributeName);
        }
    }
}

// define a custom panel type for the tutorial HTML content.
// the custom panel prepares the DOM elements of the tutorial so that they can interact
// with the Editor.
Ext.define('TutorialPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.tutorial',

    constructor: function(config) {
        Ext.apply(config || {}, {
            cls: 'tutorial',
            autoScroll:true
        });

        this.callParent(arguments);

        this.on('afterrender', function() {
            var hiddenFrame = this.el.createChild({
                id: 'tutorialIFrame',
                name: 'tutorialIFrame',     // Bugfix for firefox and IE
                tag: 'iframe',
                src: config.url,
                width: '0',
                height: '0'
            });

            hiddenFrame.on('load', function() {
                var body = hiddenFrame.dom.contentDocument.body;

                // save the URLs currently valid in the iframe
                var urls = new Ext.util.MixedCollection();
                Ext.each(body.getElementsByTagName('a'), function(a) {
                    a.id = Ext.id(a);

                    // the href property returns the absolute target URL even when the href attribute is a relative
                    // URL. The value changes when the element is moved to the container because the page has a
                    // different base URL. So store the absolute URL here and restore it later.
                    urls.add(a.id, a.href)
                }, this);


                // this is the panel node that will receive all the iframe's nodes
                var target = this.getTargetEl();
                var targetDom = target.dom;

                // remove all child nodes from the panel node - otherwise they'd get appended
                // then copy all nodes from the iframe to the panel
                if( targetDom.hasChildNodes() ) {
                    while( targetDom.childNodes.length >= 1 ) {
                        targetDom.removeChild( targetDom.firstChild );
                    }
                }
                var targetDoc = targetDom.ownerDocument;
                Ext.each(body.childNodes, function(child) {
                    targetDom.appendChild(targetDoc.importNode(child, true));
                }, this);


                // then replace the URLs we saved earlier
                Ext.each(target.query('a'), function(a) {
                    var url = urls.getByKey(a.id);
                    a.href = url;

                    var indexOfMarker = url.indexOf("/tutorial/tutorial");
                    if(indexOfMarker > 0) {
                        // this URL points inside the tutorial
                        a.target = 'tutorialIFrame';
                    } else {
                        // this URL points outside the tutorial
                        a.target = '_blank';
                    }
                }, this);


                // now update visibility and title of "Load Code" button
                Ext.each(target.query('*[class=LoadCodeButton]'), function(button) {
                    button.style.visibility = 'visible';
                    button.innerText = 'Load into Editor';
                    button.onclick = function() {
                        var url = this.getAttribute("data-url");
                        Ext.Ajax.request({
                            url: url,
                            success: function(response, opts) { Editor.setContent(response.responseText) }
                        })
                    }
                }, this);


                // control state of prev/next buttons
                var prevUrl = TutorialHelper.getTutorialDocumentData('data-prev');
                Ext.getCmp('tutorial_previous').setDisabled(prevUrl==null);
                var nextUrl = TutorialHelper.getTutorialDocumentData('data-next');
                Ext.getCmp('tutorial_next').setDisabled(nextUrl==null);


                // make sure content is scrolled to top
                Ext.getCmp('tutorial_panel').body.scrollTo('top', 0);

                Ext.getCmp('tutorial_outer').doLayout();

            }, this);

        }, this, { single: true });
    }
});


(function () {
    var tutorialStartupUrl = "/tutorial/tutorial_20_bot_01.html";
    var tutorialIndexUrl = "/tutorial/index.html";

    Tutorial = {
        create:function () {
            return Ext.create('Ext.Panel', {
                id: 'tutorial_outer',
                layout: 'fit',
                tbar:[
                    {
                        id: 'tutorial_index',
                        text: "Index",
                        handler:function () {
                            Ext.get('tutorialIFrame').dom.src = tutorialIndexUrl;
                        }
                    },
                    "-",
                    {
                        id: 'tutorial_previous',
                        text: "Previous",
                        handler:function () { Ext.get('tutorialIFrame').dom.src = TutorialHelper.getTutorialDocumentData('data-prev'); }
                    },
                    "-",
                    {
                        id: "tutorial_next",
                        text: "Next",
                        handler:function () { Ext.get('tutorialIFrame').dom.src = TutorialHelper.getTutorialDocumentData('data-next'); }
                    }
                ],

                items:[ {
                    xtype: 'tutorial',
                    id: 'tutorial_panel',
                    url: tutorialStartupUrl
                } ]
            });
        }
    };

})();