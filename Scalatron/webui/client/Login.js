/* 2012-05-04: merged this from chilicat to retain it for future use, but not yet activated in index.html. */
Ext.require(['*']);

Ext.onReady(function () {

    var lastUser;

    function login() {
        var username = lastUser;
        var password = Ext.getCmp("password").getValue();
        loginFor(username, password);
    }

    function loginFor(username, password, errorCallback) {
        lastUser = username;
        Ext.Ajax.request({
            url:"/api/users/" + username + "/session",
            method:"POST",
            headers:{
                "Accept":'application/json',
                "Content-Type":'application/json'
            },
            jsonData:{
                password:password
            },
            success:function () {
                if (username == "Administrator") {
                    window.location = "/admin/list";
                } else {
                    window.location = "/user/" + username + "/edit";
                }
            },
            failure:function (response) {
                if (errorCallback(response)) {
                    Ext.MessageBox.alert('Login failed', response.statusText);
                }
            }
        })
    }

    function enumUsers() {
        Ext.Ajax.request({
            scope:this,
            url:"/api/users/",
            method:'GET',
            headers:{
                "Accept":'application/json',
                "Content-Type":'application/json'
            },
            success:function (r) {
                var result = Ext.JSON.decode(r.responseText);
                var combo = Ext.getCmp("userName");
                combo.getStore().loadRawData(result.users);
                combo.setDisabled(false);
            }
        });
    }

    function createStore() {
        return Ext.create('Ext.data.Store', {
            fields:['name']
        });
    }

    function createUserGrid() {
        return {
            id:"userName",
            fieldLabel:'User Name',
            xtype:"grid",
            store:createStore(),
            margin:0,
            hideHeaders:true,
            border:0,
            height:100,
            columns:[
                { header:'Name', dataIndex:'name', flex:1}
            ],
            name:'user',
            displayField:'name',
            valueField:'name',
            queryMode:"local",

            editable:false,
            autoSelect:false,
            forceSelection:false,


            allowBlank:false,
            listeners:{
                afterRender:function (c) {
                    enumUsers();
                },
                itemclick:function (view, record) {
                    var username = record.data.name;
                    loginFor(username, "", function (response) {
                        if (response.status == 401) {
                            // user has password. Show password field.
                            Ext.getCmp("box").getLayout().setActiveItem(1);
                            Ext.getCmp("password").focus();
                            // prevent default handling.
                            return false;
                        }

                        // allow default handling.
                        return true;
                    });
                }
            }
        }

    }

    function createPasswordPanel() {
        return Ext.create("Ext.form.Panel", {

            layout: 'anchor',
            border: 0,

            defaults: {
                anchor: '100%'
            },
            height:100,
            defaultType:'textfield',
            padding: "0 0 4 0",

            items:[
                {
                    inputType:"password",
                    id:"password",
                    fieldLabel:'Password',
                    name:'password',
                    margin: 6,

                    listeners:{
                        scope:this,
                        specialkey:function (f, e) {
                            if (e.getKey() == e.ENTER) {
                                login();
                            } else if (e.getKey() == e.ESC) {
                                Ext.getCmp("box").getLayout().setActiveItem(0);
                            }
                        }
                    },

                    allowBlank:false
                }
           ]  ,


            buttons:[
                {
                    text:'Back',
                    id:"backButton",
                    handler:function () {
                        Ext.getCmp("box").getLayout().setActiveItem(0);
                    }
                },
                {
                    text:'Login',
                    id:"loginButton",
                    disabled:false,
                    handler:function () {
                        login();
                    }
                }

            ]
        });
    }


    Ext.create('Ext.container.Viewport', {
        layout:{
            type:'vbox',
            align:'center'
        },
        id:'root',

        items:[
            {
                xtype:"panel",
                margin:10,
                border:0,
                layout:{
                    type:'vbox',
                    align:'center'
                },
                items:[
                    Ext.create('Ext.Img', {
                        border:0,
                        src:'/header.gif'
                    })
                ],
                flex:1,
                width:700
            },

            {
                xtype:"panel",
                layout:"card",
                id:"box",

                bodyPadding:0,
                width:300,
                title:"Welcome to Scalatron",

                items:[
                    createUserGrid(),
                    createPasswordPanel()
                ]
            },
            {
                flex:1,
                border:0
            }
        ]
    });
});

