// Generated from ./LQL.g4 by ANTLR 4.9.0-SNAPSHOT


import { ParseTreeListener } from "antlr4ts/tree/ParseTreeListener";

import { ParseContext } from "./LQLParser";
import { InterfaceSpecContext } from "./LQLParser";
import { MethodSigContext } from "./LQLParser";
import { ParametersContext } from "./LQLParser";
import { InputsContext } from "./LQLParser";
import { OutputsContext } from "./LQLParser";
import { QualifiedtypeContext } from "./LQLParser";
import { SimpletypeContext } from "./LQLParser";
import { ArraytypeContext } from "./LQLParser";
import { NamedparamContext } from "./LQLParser";
import { TypeparamContext } from "./LQLParser";
import { FilterContext } from "./LQLParser";


/**
 * This interface defines a complete listener for a parse tree produced by
 * `LQLParser`.
 */
export interface LQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by `LQLParser.parse`.
	 * @param ctx the parse tree
	 */
	enterParse?: (ctx: ParseContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.parse`.
	 * @param ctx the parse tree
	 */
	exitParse?: (ctx: ParseContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.interfaceSpec`.
	 * @param ctx the parse tree
	 */
	enterInterfaceSpec?: (ctx: InterfaceSpecContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.interfaceSpec`.
	 * @param ctx the parse tree
	 */
	exitInterfaceSpec?: (ctx: InterfaceSpecContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.methodSig`.
	 * @param ctx the parse tree
	 */
	enterMethodSig?: (ctx: MethodSigContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.methodSig`.
	 * @param ctx the parse tree
	 */
	exitMethodSig?: (ctx: MethodSigContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.parameters`.
	 * @param ctx the parse tree
	 */
	enterParameters?: (ctx: ParametersContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.parameters`.
	 * @param ctx the parse tree
	 */
	exitParameters?: (ctx: ParametersContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.inputs`.
	 * @param ctx the parse tree
	 */
	enterInputs?: (ctx: InputsContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.inputs`.
	 * @param ctx the parse tree
	 */
	exitInputs?: (ctx: InputsContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.outputs`.
	 * @param ctx the parse tree
	 */
	enterOutputs?: (ctx: OutputsContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.outputs`.
	 * @param ctx the parse tree
	 */
	exitOutputs?: (ctx: OutputsContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.qualifiedtype`.
	 * @param ctx the parse tree
	 */
	enterQualifiedtype?: (ctx: QualifiedtypeContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.qualifiedtype`.
	 * @param ctx the parse tree
	 */
	exitQualifiedtype?: (ctx: QualifiedtypeContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.simpletype`.
	 * @param ctx the parse tree
	 */
	enterSimpletype?: (ctx: SimpletypeContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.simpletype`.
	 * @param ctx the parse tree
	 */
	exitSimpletype?: (ctx: SimpletypeContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.arraytype`.
	 * @param ctx the parse tree
	 */
	enterArraytype?: (ctx: ArraytypeContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.arraytype`.
	 * @param ctx the parse tree
	 */
	exitArraytype?: (ctx: ArraytypeContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.namedparam`.
	 * @param ctx the parse tree
	 */
	enterNamedparam?: (ctx: NamedparamContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.namedparam`.
	 * @param ctx the parse tree
	 */
	exitNamedparam?: (ctx: NamedparamContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.typeparam`.
	 * @param ctx the parse tree
	 */
	enterTypeparam?: (ctx: TypeparamContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.typeparam`.
	 * @param ctx the parse tree
	 */
	exitTypeparam?: (ctx: TypeparamContext) => void;

	/**
	 * Enter a parse tree produced by `LQLParser.filter`.
	 * @param ctx the parse tree
	 */
	enterFilter?: (ctx: FilterContext) => void;
	/**
	 * Exit a parse tree produced by `LQLParser.filter`.
	 * @param ctx the parse tree
	 */
	exitFilter?: (ctx: FilterContext) => void;
}

