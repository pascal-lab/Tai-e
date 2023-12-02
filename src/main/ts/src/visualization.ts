import go, { Group, Model } from 'gojs';
import { Graph } from './graph' ;
import { Alias } from 'yaml';

// const $ = go.GraphObject.make;
// const myDiagram = $(go.Diagram, "myDiagramDiv",
// {
//     // layout: $(go.GridLayout,
//     //     {
//     //         wrappingColumn: 6,
//     //         arrangement: go.GridLayout.Ascending,
//     //         spacing: new go.Size(50,50),
//     //         isRealtime: false,
//     //     })
//     layout: $(go.TreeLayout,
//         {angle: 90}
//     ),
//     "undoManager.isEnabled": true
// });

const $ = go.GraphObject.make;

const myDiagram =
new go.Diagram("myDiagramDiv",
    {
        padding: 10,
        layout: $(go.LayeredDigraphLayout,
            {
                direction: 90,
                layeringOption: go.LayeredDigraphLayout.LayerLongestPathSource,
                alignOption: go.LayeredDigraphLayout.AlignAll
            }),
        "undoManager.isEnabled": true
    });

export function visualize(graph: Graph){
    const nodeDataArray = makeNodes(graph);
    const linkDataArray = makeLinks(graph);

    myDiagram.model = new go.GraphLinksModel(nodeDataArray, linkDataArray);

    myDiagram.nodeTemplate = 
        $(go.Node, "Auto",
            new go.Binding("visible"),
            $(go.Shape, "Rectangle", {fill: "lightblue"}, 
                new go.Binding("fill", "color"),
                new go.Binding("stroke", "isHighlighted", h => h ? "red" : "black").ofObject(),
                new go.Binding("strokeWidth", "isHighlighted", h => h ? 4 : 2).ofObject(),
            ),
            $(go.TextBlock, "isla", { margin: 10 },
                new go.Binding("text", "key")
            ),
            {
                selectionAdornmentTemplate:
                  $(go.Adornment, "Spot",
                    $(go.Panel, "Auto",
                        $(go.Shape, { fill: null, stroke: "black", strokeWidth: 4 }),
                        $(go.Placeholder),
                    ),
                    $("Button",
                        $(go.TextBlock, "trace"),
                        { alignment: go.Spot.Bottom, alignmentFocus: go.Spot.Center, desiredSize: new go.Size(70,25), click: highlightPath },
                    )
                  ),
            },
            $("Button",  // a replacement for "TreeExpanderButton" that works for non-tree-structured graphs
                // assume initially not visible because there are no links coming out
                { visible: false },
                // bind the button visibility to whether it's not a leaf node
                new go.Binding("visible", "isTreeLeaf", leaf => !leaf).ofObject(),
                $(go.Shape,
                    {
                        name: "ButtonIcon",
                        figure: "MinusLine",
                        desiredSize: new go.Size(6, 6)
                    },
                new go.Binding("figure", "isCollapsed",  // data.isCollapsed remembers "collapsed" or "expanded"
                                collapsed => collapsed ? "PlusLine" : "MinusLine")),
                {
                    click: (e, obj) => {
                        e.diagram.startTransaction();
                        var node = obj.part as go.Node;
                        if (node.data.isCollapsed) {
                            expandFrom(node, node);
                        } else {
                            collapseFrom(node, node);
                        }
                        e.diagram.commitTransaction("toggled visibility of dependencies");
                    }
                }
            )
        );
    
    myDiagram.linkTemplate = 
        $(go.Link,
            { curve: go.Link.Bezier },
            $(go.Shape, { strokeWidth: 3 },
                new go.Binding("stroke", "isHighlighted", h => h ? "red" : "black").ofObject(),
                new go.Binding("strokeWidth", "isHighlighted", h => h ? 4 : 2).ofObject(),
            ),
            $(go.Shape, { toArrow: "Standard", strokeWidth: 3 },
                new go.Binding("stroke", "isHighlighted", h => h ? "red" : "black").ofObject(),
            ),
        );

    myDiagram.groupTemplate =
        $(go.Group, "Auto",
            {
                // layout: $(go.GridLayout,
                // { wrappingColumn: 2, arrangement: go.GridLayout.Ascending, spacing: new go.Size(50,50), isRealtime: false }),
                // isSubGraphExpanded: false,
                layout: $(go.LayeredDigraphLayout,
                    {
                      direction: 90,
                      layeringOption: go.LayeredDigraphLayout.LayerLongestPathSource,
                      alignOption: go.LayeredDigraphLayout.AlignAll
                    }),
                    isSubGraphExpanded: false
            },
            $(go.Shape, "RoundedRectangle",
                { fill: null, stroke: "gray", strokeWidth: 2 }),
            $(go.Panel, "Vertical",
                { defaultAlignment: go.Spot.Left, margin: 4 },
                $(go.Panel, "Horizontal",
                    { defaultAlignment: go.Spot.Top },
                    $("SubGraphExpanderButton"),
                    $(go.TextBlock,
                        { font: "Bold 18px Sans-Serif", margin: 4 },
                        new go.Binding("text", "key")),
                ),
                $(go.Placeholder,
                { padding: new go.Margin(0, 10) }),
            ),
        );
    myDiagram.nodes.each(function(n) {
        // myDiagram.startTransaction();
        n.visible = false; 
        // myDiagram.commitTransaction("hide all nodes");
    })
    myDiagram.nodes.each(function(n) {
        if(n instanceof go.Node && !(n instanceof go.Group) && !n.findLinksInto().next()){
            var nd = n as go.Node;
            nd.visible = true;
            while(nd.containingGroup !== null){
                (nd.containingGroup as go.Group).visible = true;
                nd = nd.containingGroup;
            }
        }
    })
    myDiagram.links.each(link => link.visible = false);

    if(!buttonCreated){
        LayersChangeButton("Remove Package", "Restore Package", graph, restorePackages, removePackages);
        LayersChangeButton("Remove Classes", "Restore Class", graph, restoreClasses, removeClasses);
        LayersChangeButton("Remove Methods", "Restore Method", graph, restoreMethods, removeMethods);

        HighlightPathButton("Highlight Recommanded Paths", graph, highlightRecommandedPath);

        ResetButton(graph, hideAll);
        buttonCreated = true;
    }
    
    // myDiagram.layout = $(go.TreeLayout, { angle: 90 });

    // myDiagram.model = new go.GraphLinksModel(
    //     [ { key: 1 },
    //       { key: 2 },
    //       { key: 3 },
    //     ],
    //     [ { from: 1, to: 3 },
    //       { from: 2, to: 3 }] );
    // myDiagram.nodes.each(function(n) {
    //     n.wasTreeExpanded = false; 
    //     n.isTreeExpanded = false;
    // })
}

// ===================== LayersChangeButton Function =========================

var buttonCreated = false;
// 记录各层次是否还在图中出现，如packageExist = false代表图中不出现Package层次
let packageExist = true;
let classExist = true;
let methodExist = true;
function LayersChangeButton(restoreState : string, removeState : string, graph : Graph, restoreFunc : (graph : Graph) => void, removeFunc : (graph : Graph) => void){
    var button = document.createElement("button");
    button.innerHTML = restoreState;
    document.getElementById("buttonContainer")?.appendChild(button);
    button.addEventListener("click", function(){
        if(button.innerHTML === restoreState){
            button.innerHTML = removeState;
            removeFunc(graph);
        }
        else if(button.innerHTML === removeState){
            button.innerHTML = restoreState;
            restoreFunc(graph);
        }
    })
}

function removePackages(graph : Graph){
    myDiagram.startTransaction("remove Package");
    graph.metadata.packages.forEach(removeGroup);
    myDiagram.commitTransaction("remove Package");
    packageExist = false;
}

function removeClasses(graph : Graph){
    myDiagram.startTransaction("remove Class");
    graph.metadata.classes.forEach(removeGroup);
    myDiagram.commitTransaction("remove Class");
    classExist = false;
}

function removeMethods(graph : Graph){
    myDiagram.startTransaction("remove Method");
    graph.metadata.methods.forEach(removeGroup);
    myDiagram.commitTransaction("remove Method");
    methodExist = false;
}

function removeGroup(name: string){
    var group = myDiagram.findNodeForKey(name) as go.Group;
    if(group === null) return;

    (group.diagram as go.Diagram).model.setDataProperty(group, "isSubGraphExpanded", true);
    group.memberParts.each(nodeOrGroup => (nodeOrGroup.diagram as go.Diagram).model.setDataProperty(nodeOrGroup.data, "group", group.containingGroup? group.containingGroup.key : null));
    myDiagram.model.removeNodeData(myDiagram.model.findNodeDataForKey(name) as go.ObjectData);
}


function restorePackages(graph : Graph){
    myDiagram.startTransaction("restore Package");
    // 恢复Package层需要先恢复Class层，而后再将Class层删去，做到只恢复Package层的效果
    if(!classExist){ 
        restoreClasses(graph);
        graph.relation.packageToClasses.forEach((vc,kp) => restoreGroup(graph.metadata.getp(kp), vc.filter(v => v !== undefined).map(v => graph.metadata.getc(v))));
        removeClasses(graph);
    }else{
        graph.relation.packageToClasses.forEach((vc,kp) => restoreGroup(graph.metadata.getp(kp), vc.filter(v => v !== undefined).map(v => graph.metadata.getc(v))));
    }
    myDiagram.commitTransaction("restore Package");
    packageExist = true;
}

function restoreClasses(graph : Graph){
    myDiagram.startTransaction("restore Class");
    // 恢复Class层需要先恢复Method层，理由同上
    if(!methodExist){
        restoreMethods(graph);
        graph.relation.classToMethods.forEach((vm,kc) => restoreGroup(graph.metadata.getc(kc), vm.filter(v => v !== undefined).map(v => graph.metadata.getm(v))));
        removeMethods(graph);
    }else{
        graph.relation.classToMethods.forEach((vm,kc) => restoreGroup(graph.metadata.getc(kc), vm.filter(v => v !== undefined).map(v => graph.metadata.getm(v))));
    }
    myDiagram.commitTransaction("restore Class");
    classExist = true;
}

function restoreMethods(graph : Graph){
    myDiagram.startTransaction("restore Method");
    graph.relation.methodToVars.forEach((vv,km) => 
        restoreGroup(graph.metadata.getm(km), vv.filter(v => v !== undefined).map(v => graph.metadata.getvf(v))));
    myDiagram.commitTransaction("restore Method");
    methodExist = true;
}

function restoreGroup(name: string, members : string[]){
    myDiagram.model.addNodeData({key: name, isGroup: true, isHighlighted: false, isCollapsed: false});

    const parent = myDiagram.findNodeForKey(name) as go.Group;  // 要恢复的Group
    let containGroupKey;

    // 将要恢复的Group展开
    (parent.diagram as go.Diagram).model.setDataProperty(parent, "isSubGraphExpanded", true);
    members.forEach(n=>{
        var node = myDiagram.findNodeForKey(n) as go.Node;
        if(node === null){
            console.error("No such node found in Diagram");
            return;
        }
        containGroupKey = node.containingGroup?.key;
        (node.diagram as go.Diagram).model.setDataProperty(node.data, "group", name);
    });

    // 如果发现恢复的Group内部没有任何成员可见，那么折叠这个Group并使其不可见
    if(parent.memberParts.all(m => !m.isVisible())){
        (parent.diagram as go.Diagram).model.setDataProperty(parent, "isSubGraphExpanded", false);
        (parent.diagram as go.Diagram).model.setDataProperty(parent, "visible", false);
    }

    // 设置恢复Group的containingGroup
    (parent.diagram as go.Diagram).model.setDataProperty(parent.data, "group", containGroupKey);
}

// ====================== HighlightPathButton ====================

// todo: 找到对所有路径的合适可视化方式
function HighlightPathButton(text: string, graph: Graph, highlight: (graph : Graph) => void){
    var button = document.createElement("button");
    button.innerHTML = text;
    document.getElementById("buttonContainer")?.appendChild(button);
    button.addEventListener("click", function(){
        highlight(graph);
    })
}

function highlightRecommandedPath(graph: Graph){
    myDiagram.startTransaction("highlight recommanded paths");
    graph.recommendedPaths.forEach(path=>{
        // path.forEach(n=>{
        //     let node = myDiagram.findNodeForKey(graph.metadata.getvf(n));
        //     if(node !== null) {
        //         node.diagram?.model.setDataProperty(node.data, "isHighlighted", true);
        //     }
        // })
        for(var n = 0; n < path.length - 1; n++){
            const currNode = myDiagram.findNodeForKey(graph.metadata.getvf(path[n]));
            const nextNode = myDiagram.findNodeForKey(graph.metadata.getvf(path[n + 1]));
            if(currNode === null || nextNode === null){
                console.error("Nodes not found, isField-n: " + graph.isField(path[n]) + ", isField-(n+1)" + graph.isField(path[n + 1]));
                return;
            }

            currNode.diagram?.model.setDataProperty(currNode, "isHighlighted", true);
            currNode.diagram?.model.setDataProperty(currNode.data, "visible", true);
            currNode.diagram?.model.setDataProperty(currNode.data, "isCollapsed", false);
            var cg = currNode.containingGroup;
            while(cg !== null && cg.visible !== true){
                (cg.diagram as go.Diagram).model.setDataProperty(cg, "visible", true);
                cg = cg.containingGroup;
            }
            
            
            currNode.findLinksTo(nextNode).each(l => {
                l.diagram?.model.setDataProperty(l, "isHighlighted", true);
                l.diagram?.model.setDataProperty(l, "visible", true);
            });
        }

        const lastNode = myDiagram.findNodeForKey(graph.metadata.getvf(path[path.length - 1]));
        if(lastNode === null){
            console.error("Nodes not found, isField-n: " + graph.isField(path[path.length - 1]));
            return;
        }
        lastNode.diagram?.model.setDataProperty(lastNode, "isHighlighted", true);
        lastNode.diagram?.model.setDataProperty(lastNode.data, "visible", true);
        lastNode.diagram?.model.setDataProperty(lastNode.data, "isCollapsed", false);
        var cg = lastNode.containingGroup;
        while(cg !== null && cg.visible !== true){
            (cg.diagram as go.Diagram).model.setDataProperty(cg, "visible", true);
            cg = cg.containingGroup;
        }
    })
    myDiagram.commitTransaction("highlight recommanded paths");
}

// ===================== ResetButton ========================

function ResetButton(graph: Graph, hide: (graph: Graph) => void){
    var button = document.createElement("button");
    button.innerHTML = "Reset";
    document.getElementById("buttonContainer")?.appendChild(button);
    button.addEventListener("click", function(){
        hide(graph);
    })
}

function hideAll(graph: Graph){
    myDiagram.startTransaction("hide All");
    myDiagram.nodes.each(n=>{
        if(n instanceof go.Node) {
            n.diagram?.model.setDataProperty(n.data, "isCollapsed", true);
            n.diagram?.model.setDataProperty(n.data, "visible", false);
            while(n.containingGroup !== null){
                let nd = n.containingGroup as go.Group;
                if(nd.memberParts.any(m => m.isVisible())) break;
                nd.diagram?.model.setDataProperty(nd, "isSubGraphExpanded", false);
                nd.diagram?.model.setDataProperty(nd, "visible", false);
                n = nd;
            }
        }
    });
    myDiagram.links.each(l=>l.diagram?.model.setDataProperty(l, "visible", false));
    graph.sourceNodes.forEach(num => {
        const s = graph.metadata.getvf(num);
        var n = myDiagram.findNodeForKey(s) as go.Node;
        n.diagram?.model.setDataProperty(n, "visible", true);
        while(n.containingGroup !== null){
            let nd = n.containingGroup as go.Group;
            nd.diagram?.model.setDataProperty(nd, "isSubGraphExpanded", false);
            nd.diagram?.model.setDataProperty(nd, "visible", true);
            n = nd;
        }
    })
    myDiagram.commitTransaction("hide All");
    // visualize(graph);
}

// ===============================================================
// ===============================================================

myDiagram.click = e => {
    e.diagram.commit(d => d.clearHighlighteds(), "clear highlights");
}

// ====================== Fold & Expand Event ====================
function collapseFrom(node: go.Node, start: go.Node) {
    // 若该节点还有可见的入边，则保留
    if (node.findLinksInto().any(link => link.visible) && node !== start){
        return;
    }

    if (node.data.isCollapsed) {
        collapse(node);
        return;
    }

    (node.diagram as go.Diagram).model.setDataProperty(node.data, "isCollapsed", true);

    if (node !== start) {
        collapse(node);
    }
    node.findLinksOutOf().each(link => (link.diagram as go.Diagram).model.setDataProperty(link, "visible", false))
    node.findNodesOutOf().each(n => collapseFrom(n, start));

    function collapse(node: go.Node){
        (node.diagram as go.Diagram).model.setDataProperty(node.data, "visible", false);
        
        // 若Group内没有可见的Node，则Group也隐藏
        var cg = node.containingGroup;
        while(cg !== null){
            if (cg.memberParts.any(n => n.visible)) {
                break;
            } 

            (cg.diagram as go.Diagram).model.setDataProperty(cg, "visible", false);
            cg = cg.containingGroup;
        }
    }
}

function expandFrom(node: go.Node, start: go.Node) {
    if (!node.data.isCollapsed) 
        return;

    (node.diagram as go.Diagram).model.setDataProperty(node.data, "isCollapsed", false);

    node.findNodesOutOf().each(n => {
            (n.diagram as go.Diagram).model.setDataProperty(n.data, "visible", true);

            // 若有Node变为可见，则Node所在的Group也得可见
            var cg = n.containingGroup;
            while(cg !== null && cg.visible !== true){
                (cg.diagram as go.Diagram).model.setDataProperty(cg, "visible", true);
                cg = cg.containingGroup;
            }
        }
    )
    node.findLinksOutOf().each(link => (link.diagram as go.Diagram).model.setDataProperty(link, "visible", true))
}
// ===============================================================


function highlightPath(event: go.InputEvent, ador: go.GraphObject){
    const diagram = ador.diagram as go.Diagram;
    const node = ((ador.part as go.Adornment).adornedPart) as go.Node;
    diagram.startTransaction("highlight");
    diagram.clearHighlighteds();
    diagram.findLayer("Foreground")?.parts.each(p=>{
        if(p instanceof go.Link){
            p.layerName = "";
        }
    })
    mark(node, 4);
    diagram.commitTransaction("highlight");
}
function mark(node: go.Node, depth: number){
    if(depth === 0) return;

    node.isHighlighted = true;
    node.findLinksOutOf().each(l =>{
        l.isHighlighted = true;
        l.layerName = "Foreground";
    });
    node.findNodesOutOf().each(n => mark(n, depth - 1));
}



function makeNodes(graph: Graph){
    const nodeDataArray: INode[] = [];
    
    graph.relation.packageToClasses.forEach((vc,kp)=>{
        const pName:string = graph.metadata.getp(kp)

        nodeDataArray.push({key: pName, isGroup: true, isHighlighted: false, isCollapsed: true});
        vc.forEach(n=>{
            if(graph.relation.classToMethods.get(n) !== undefined){
                nodeDataArray.push({key:graph.metadata.getc(n), isGroup: true, group: pName, isHighlighted: false, isCollapsed: true});
            }
        });
    })

    graph.relation.classToMethods.forEach((vm, kc)=>{
        const cName:string = graph.metadata.getc(kc);
        vm.forEach(n=>{
            if(graph.relation.methodToVars.get(n) !== undefined){
                nodeDataArray.push({key:graph.metadata.getm(n), isGroup: true, group: cName, isHighlighted: false, isCollapsed: true});
            }
        });
    })

    graph.relation.methodToVars.forEach((vv, km)=>{
        const mName:string = graph.metadata.getm(km);
        vv.forEach(n=>{
            const color:string = graph.isSource(n)? 'gold' : (graph.isSink(n)? 'aquamarine' : 'lightblue');
            nodeDataArray.push({key: graph.metadata.getvf(n), color: color, group: mName, isHighlighted: false, isCollapsed: true});
        });
    })
    console.log('finish nodes making');
    return nodeDataArray;
}

function makeLinks(graph: Graph){
    const linkDataArray: ILink[] = [];

    graph.graph.forEach((v,k)=>{
        v.forEach(n=>linkDataArray.push({from: graph.metadata.getvf(k), to: graph.metadata.getvf(n), color: 'black', isHighlighted: false, zOrder: 0}));
    })
    console.log('finish links making');
    return linkDataArray;
}

interface INode{
    key?: string;
    color?: string;
    isGroup?: boolean;
    group?: string;
    isHighlighted: boolean;
    isCollapsed: boolean;
}

interface ILink{
    key?: string;
    from: string;
    to: string;
    color?: string;
    isHighlighted: boolean;
    zOrder?: number;
}