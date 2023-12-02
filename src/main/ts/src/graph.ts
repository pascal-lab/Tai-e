import { parse } from "yaml";

export class Graph{
    metadata: Metadata;
    relation: Relation;
    graph: Map<number, number[]>;
    sourceNodes: number[];
    sinkNodes: number[];
    recommendedPaths: number[][];

    constructor(data: any){
        this.metadata = new Metadata(data.metadata);
        this.relation = new Relation(data.relation, this.metadata);
        this.graph = new Map();
        this.initGraph(data.graph);
        this.sourceNodes = data.sourceNodes;
        this.sinkNodes = data.sinkNodes;
        
        this.recommendedPaths = (data.recommendedPaths as number[][]).map(path=>path.filter(n => !this.isField(n)));
        // console.log(this.recommandedPaths);
        // console.log(this.sourceNodes + "  " + this.sinkNodes)
        // console.log(this.metadata.getvf(6))
        // console.log(this.metadata.getvf(25))
        // console.log(this.metadata.getvf(26))
        // console.log(this.metadata.getvf(27))
    }

    initGraph(graph: any){
        Object.entries(graph).forEach(entry=>{
            const toNodess: number[] = [];
            (entry[1] as number[]).forEach(n=>{
                if(this.isField(n)){
                    toNodess.push(...(graph[n] as number[]));
                }else{
                    toNodess.push(n);
                }
            });
            const toNodes = Array.from(new Set(toNodess));
            this.graph.set(parseInt(entry[0]), toNodes);
        });
    }

    isField(num:number){
        return this.relation.fields.includes(num);
    }

    isSource(num:number){
        return this.sourceNodes.includes(num);
    }

    isSink(num:number){
        return this.sinkNodes.includes(num);
    }
}

class Metadata{
    packages: string[];
    classes: string[];
    methods: string[];
    varsAndFields: string[];

    constructor(metadata: any){
        this.packages = metadata.packages;
        this.classes = metadata.classes;
        this.methods = metadata.methods;
        this.varsAndFields = metadata.varsAndFields;

        const len: number = this.classes.length;
        for(let i = 0; i < len; i++){
            if(!this.classes[i].includes('.')){
                // set the class with no parent package different to its package
                this.classes[i] = '.' + this.classes[i];
            }
        }
    }

    getp(index: number) {
        return this.packages[index];
    }

    getc(index: number) {
        return this.classes[index];
    }

    getm(index: number) {
        return this.methods[index];
    }

    getvf(index: number) {
        return this.varsAndFields[index];
    }
}

class Relation{
    packageToClasses: Map<number, number[]>;
    classToMethods: Map<number, number[]>;
    methodToVars: Map<number, number[]>;

    fields: number[];
    // jdkVars: number[];

    constructor(relation: any, metadata: Metadata){
        this.packageToClasses = new Map();
        this.classToMethods = new Map();
        this.methodToVars = new Map();
        this.fields = Object.values(relation.classToFields).flat() as number[];

        // const jdkPackages: number[] = [];
        Object.entries(relation.packageToClasses).forEach(entry=>{
            this.packageToClasses.set(parseInt(entry[0]), entry[1] as number[]);
            // if(metadata.getp(parseInt(entry[0])) === "java.lang"){ // todo:
            //     jdkPackages.push(parseInt(entry[0]));
            // }
        });

        Object.entries(relation.classToMethods).forEach(entry=>{
            this.classToMethods.set(parseInt(entry[0]), entry[1] as number[]);
        });

        Object.entries(relation.methodToVars).forEach(entry=>{
            this.methodToVars.set(parseInt(entry[0]), entry[1] as number[]);
        });

        // const jdkClasses: number[] = (Array.from(jdkPackages, p => this.packageToClasses.get(p)).flat().filter(item => item !== undefined) as number[]);
        // const jdkMethods: number[] = (Array.from(jdkClasses, p => this.classToMethods.get(p)).flat().filter(item => item !== undefined) as number[]);
        // this.jdkVars = (Array.from(jdkMethods, p => this.methodToVars.get(p)).flat().filter(item => item !== undefined) as number[]);
    }

}

export function build(file:File, visualize:(graph:Graph)=>void){
    const reader = new FileReader();
    
    reader.onload = function(e){
        const yamlContent = e.target?.result;
        if(typeof yamlContent != 'string'){
            console.log('yamlContent不是字符串类型');
            return;
        }
        // try{
            const parsedData = parse((yamlContent as string));
            const graph:Graph = new Graph(parsedData);
            console.log('finish build, begin visualization');
            visualize(graph);
        // }
        // catch{
        //     console.log(e);
        // }
    }
    
    reader.readAsText(file);
}