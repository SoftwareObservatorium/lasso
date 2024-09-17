// Generated from ./LQL.g4 by ANTLR 4.9.0-SNAPSHOT


import { ATN } from "antlr4ts/atn/ATN";
import { ATNDeserializer } from "antlr4ts/atn/ATNDeserializer";
import { FailedPredicateException } from "antlr4ts/FailedPredicateException";
import { NotNull } from "antlr4ts/Decorators";
import { NoViableAltException } from "antlr4ts/NoViableAltException";
import { Override } from "antlr4ts/Decorators";
import { Parser } from "antlr4ts/Parser";
import { ParserRuleContext } from "antlr4ts/ParserRuleContext";
import { ParserATNSimulator } from "antlr4ts/atn/ParserATNSimulator";
import { ParseTreeListener } from "antlr4ts/tree/ParseTreeListener";
import { ParseTreeVisitor } from "antlr4ts/tree/ParseTreeVisitor";
import { RecognitionException } from "antlr4ts/RecognitionException";
import { RuleContext } from "antlr4ts/RuleContext";
//import { RuleVersion } from "antlr4ts/RuleVersion";
import { TerminalNode } from "antlr4ts/tree/TerminalNode";
import { Token } from "antlr4ts/Token";
import { TokenStream } from "antlr4ts/TokenStream";
import { Vocabulary } from "antlr4ts/Vocabulary";
import { VocabularyImpl } from "antlr4ts/VocabularyImpl";

import * as Utils from "antlr4ts/misc/Utils";

import { LQLListener } from "./LQLListener";

export class LQLParser extends Parser {
	public static readonly T__0 = 1;
	public static readonly T__1 = 2;
	public static readonly T__2 = 3;
	public static readonly T__3 = 4;
	public static readonly T__4 = 5;
	public static readonly T__5 = 6;
	public static readonly T__6 = 7;
	public static readonly T__7 = 8;
	public static readonly T__8 = 9;
	public static readonly T__9 = 10;
	public static readonly T__10 = 11;
	public static readonly NAME = 12;
	public static readonly FILTERVALUE = 13;
	public static readonly TEXT = 14;
	public static readonly SPACE = 15;
	public static readonly RULE_parse = 0;
	public static readonly RULE_interfaceSpec = 1;
	public static readonly RULE_methodSig = 2;
	public static readonly RULE_parameters = 3;
	public static readonly RULE_inputs = 4;
	public static readonly RULE_outputs = 5;
	public static readonly RULE_qualifiedtype = 6;
	public static readonly RULE_simpletype = 7;
	public static readonly RULE_arraytype = 8;
	public static readonly RULE_namedparam = 9;
	public static readonly RULE_typeparam = 10;
	public static readonly RULE_filter = 11;
	// tslint:disable:no-trailing-whitespace
	public static readonly ruleNames: string[] = [
		"parse", "interfaceSpec", "methodSig", "parameters", "inputs", "outputs", 
		"qualifiedtype", "simpletype", "arraytype", "namedparam", "typeparam", 
		"filter",
	];

	private static readonly _LITERAL_NAMES: Array<string | undefined> = [
		undefined, "'{'", "'}'", "'('", "')->'", "')'", "','", "'.'", "'[]'", 
		"'='", "'<'", "'>'",
	];
	private static readonly _SYMBOLIC_NAMES: Array<string | undefined> = [
		undefined, undefined, undefined, undefined, undefined, undefined, undefined, 
		undefined, undefined, undefined, undefined, undefined, "NAME", "FILTERVALUE", 
		"TEXT", "SPACE",
	];
	public static readonly VOCABULARY: Vocabulary = new VocabularyImpl(LQLParser._LITERAL_NAMES, LQLParser._SYMBOLIC_NAMES, []);

	// @Override
	// @NotNull
	public get vocabulary(): Vocabulary {
		return LQLParser.VOCABULARY;
	}
	// tslint:enable:no-trailing-whitespace

	// @Override
	public get grammarFileName(): string { return "LQL.g4"; }

	// @Override
	public get ruleNames(): string[] { return LQLParser.ruleNames; }

	// @Override
	public get serializedATN(): string { return LQLParser._serializedATN; }

	protected createFailedPredicateException(predicate?: string, message?: string): FailedPredicateException {
		return new FailedPredicateException(this, predicate, message);
	}

	constructor(input: TokenStream) {
		super(input);
		this._interp = new ParserATNSimulator(LQLParser._ATN, this);
	}
	// @RuleVersion(0)
	public parse(): ParseContext {
		let _localctx: ParseContext = new ParseContext(this._ctx, this.state);
		this.enterRule(_localctx, 0, LQLParser.RULE_parse);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 25;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			if (_la === LQLParser.NAME) {
				{
				this.state = 24;
				this.interfaceSpec();
				}
			}

			this.state = 30;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			while (_la === LQLParser.FILTERVALUE) {
				{
				{
				this.state = 27;
				this.filter();
				}
				}
				this.state = 32;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
			}
			this.state = 33;
			this.match(LQLParser.EOF);
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public interfaceSpec(): InterfaceSpecContext {
		let _localctx: InterfaceSpecContext = new InterfaceSpecContext(this._ctx, this.state);
		this.enterRule(_localctx, 2, LQLParser.RULE_interfaceSpec);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 37;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 2, this._ctx) ) {
			case 1:
				{
				this.state = 35;
				this.simpletype();
				}
				break;

			case 2:
				{
				this.state = 36;
				this.qualifiedtype();
				}
				break;
			}
			this.state = 39;
			this.match(LQLParser.T__0);
			this.state = 43;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			while (_la === LQLParser.NAME) {
				{
				{
				this.state = 40;
				this.methodSig();
				}
				}
				this.state = 45;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
			}
			this.state = 46;
			this.match(LQLParser.T__1);
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public methodSig(): MethodSigContext {
		let _localctx: MethodSigContext = new MethodSigContext(this._ctx, this.state);
		this.enterRule(_localctx, 4, LQLParser.RULE_methodSig);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 48;
			this.match(LQLParser.NAME);
			this.state = 62;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 7, this._ctx) ) {
			case 1:
				{
				this.state = 49;
				this.match(LQLParser.T__2);
				this.state = 51;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
				if (_la === LQLParser.NAME) {
					{
					this.state = 50;
					this.inputs();
					}
				}

				this.state = 53;
				this.match(LQLParser.T__3);
				this.state = 55;
				this._errHandler.sync(this);
				switch ( this.interpreter.adaptivePredict(this._input, 5, this._ctx) ) {
				case 1:
					{
					this.state = 54;
					this.outputs();
					}
					break;
				}
				}
				break;

			case 2:
				{
				this.state = 57;
				this.match(LQLParser.T__2);
				this.state = 59;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
				if (_la === LQLParser.NAME) {
					{
					this.state = 58;
					this.inputs();
					}
				}

				this.state = 61;
				this.match(LQLParser.T__4);
				}
				break;
			}
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public parameters(): ParametersContext {
		let _localctx: ParametersContext = new ParametersContext(this._ctx, this.state);
		this.enterRule(_localctx, 6, LQLParser.RULE_parameters);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 69;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 8, this._ctx) ) {
			case 1:
				{
				this.state = 64;
				this.simpletype();
				}
				break;

			case 2:
				{
				this.state = 65;
				this.qualifiedtype();
				}
				break;

			case 3:
				{
				this.state = 66;
				this.arraytype();
				}
				break;

			case 4:
				{
				this.state = 67;
				this.namedparam();
				}
				break;

			case 5:
				{
				this.state = 68;
				this.typeparam();
				}
				break;
			}
			this.state = 81;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			while (_la === LQLParser.T__5) {
				{
				{
				this.state = 71;
				this.match(LQLParser.T__5);
				this.state = 77;
				this._errHandler.sync(this);
				switch ( this.interpreter.adaptivePredict(this._input, 9, this._ctx) ) {
				case 1:
					{
					this.state = 72;
					this.simpletype();
					}
					break;

				case 2:
					{
					this.state = 73;
					this.qualifiedtype();
					}
					break;

				case 3:
					{
					this.state = 74;
					this.arraytype();
					}
					break;

				case 4:
					{
					this.state = 75;
					this.namedparam();
					}
					break;

				case 5:
					{
					this.state = 76;
					this.typeparam();
					}
					break;
				}
				}
				}
				this.state = 83;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
			}
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public inputs(): InputsContext {
		let _localctx: InputsContext = new InputsContext(this._ctx, this.state);
		this.enterRule(_localctx, 8, LQLParser.RULE_inputs);
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 84;
			this.parameters();
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public outputs(): OutputsContext {
		let _localctx: OutputsContext = new OutputsContext(this._ctx, this.state);
		this.enterRule(_localctx, 10, LQLParser.RULE_outputs);
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 86;
			this.parameters();
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public qualifiedtype(): QualifiedtypeContext {
		let _localctx: QualifiedtypeContext = new QualifiedtypeContext(this._ctx, this.state);
		this.enterRule(_localctx, 12, LQLParser.RULE_qualifiedtype);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 88;
			this.match(LQLParser.NAME);
			this.state = 93;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			while (_la === LQLParser.T__6) {
				{
				{
				this.state = 89;
				this.match(LQLParser.T__6);
				this.state = 90;
				this.match(LQLParser.NAME);
				}
				}
				this.state = 95;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
			}
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public simpletype(): SimpletypeContext {
		let _localctx: SimpletypeContext = new SimpletypeContext(this._ctx, this.state);
		this.enterRule(_localctx, 14, LQLParser.RULE_simpletype);
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 96;
			this.match(LQLParser.NAME);
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public arraytype(): ArraytypeContext {
		let _localctx: ArraytypeContext = new ArraytypeContext(this._ctx, this.state);
		this.enterRule(_localctx, 16, LQLParser.RULE_arraytype);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 100;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 12, this._ctx) ) {
			case 1:
				{
				this.state = 98;
				this.simpletype();
				}
				break;

			case 2:
				{
				this.state = 99;
				this.qualifiedtype();
				}
				break;
			}
			this.state = 105;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			while (_la === LQLParser.T__7) {
				{
				{
				this.state = 102;
				this.match(LQLParser.T__7);
				}
				}
				this.state = 107;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
			}
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public namedparam(): NamedparamContext {
		let _localctx: NamedparamContext = new NamedparamContext(this._ctx, this.state);
		this.enterRule(_localctx, 18, LQLParser.RULE_namedparam);
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 108;
			this.match(LQLParser.NAME);
			this.state = 109;
			this.match(LQLParser.T__8);
			this.state = 113;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 14, this._ctx) ) {
			case 1:
				{
				this.state = 110;
				this.simpletype();
				}
				break;

			case 2:
				{
				this.state = 111;
				this.qualifiedtype();
				}
				break;

			case 3:
				{
				this.state = 112;
				this.arraytype();
				}
				break;
			}
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public typeparam(): TypeparamContext {
		let _localctx: TypeparamContext = new TypeparamContext(this._ctx, this.state);
		this.enterRule(_localctx, 20, LQLParser.RULE_typeparam);
		let _la: number;
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 117;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 15, this._ctx) ) {
			case 1:
				{
				this.state = 115;
				this.simpletype();
				}
				break;

			case 2:
				{
				this.state = 116;
				this.qualifiedtype();
				}
				break;
			}
			this.state = 119;
			this.match(LQLParser.T__9);
			this.state = 123;
			this._errHandler.sync(this);
			switch ( this.interpreter.adaptivePredict(this._input, 16, this._ctx) ) {
			case 1:
				{
				this.state = 120;
				this.simpletype();
				}
				break;

			case 2:
				{
				this.state = 121;
				this.qualifiedtype();
				}
				break;

			case 3:
				{
				this.state = 122;
				this.typeparam();
				}
				break;
			}
			this.state = 133;
			this._errHandler.sync(this);
			_la = this._input.LA(1);
			while (_la === LQLParser.T__5) {
				{
				{
				this.state = 125;
				this.match(LQLParser.T__5);
				this.state = 129;
				this._errHandler.sync(this);
				switch ( this.interpreter.adaptivePredict(this._input, 17, this._ctx) ) {
				case 1:
					{
					this.state = 126;
					this.simpletype();
					}
					break;

				case 2:
					{
					this.state = 127;
					this.qualifiedtype();
					}
					break;

				case 3:
					{
					this.state = 128;
					this.typeparam();
					}
					break;
				}
				}
				}
				this.state = 135;
				this._errHandler.sync(this);
				_la = this._input.LA(1);
			}
			this.state = 136;
			this.match(LQLParser.T__10);
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}
	// @RuleVersion(0)
	public filter(): FilterContext {
		let _localctx: FilterContext = new FilterContext(this._ctx, this.state);
		this.enterRule(_localctx, 22, LQLParser.RULE_filter);
		try {
			this.enterOuterAlt(_localctx, 1);
			{
			this.state = 138;
			this.match(LQLParser.FILTERVALUE);
			}
		}
		catch (re) {
			if (re instanceof RecognitionException) {
				_localctx.exception = re;
				this._errHandler.reportError(this, re);
				this._errHandler.recover(this, re);
			} else {
				throw re;
			}
		}
		finally {
			this.exitRule();
		}
		return _localctx;
	}

	public static readonly _serializedATN: string =
		"\x03\uC91D\uCABA\u058D\uAFBA\u4F53\u0607\uEA8B\uC241\x03\x11\x8F\x04\x02" +
		"\t\x02\x04\x03\t\x03\x04\x04\t\x04\x04\x05\t\x05\x04\x06\t\x06\x04\x07" +
		"\t\x07\x04\b\t\b\x04\t\t\t\x04\n\t\n\x04\v\t\v\x04\f\t\f\x04\r\t\r\x03" +
		"\x02\x05\x02\x1C\n\x02\x03\x02\x07\x02\x1F\n\x02\f\x02\x0E\x02\"\v\x02" +
		"\x03\x02\x03\x02\x03\x03\x03\x03\x05\x03(\n\x03\x03\x03\x03\x03\x07\x03" +
		",\n\x03\f\x03\x0E\x03/\v\x03\x03\x03\x03\x03\x03\x04\x03\x04\x03\x04\x05" +
		"\x046\n\x04\x03\x04\x03\x04\x05\x04:\n\x04\x03\x04\x03\x04\x05\x04>\n" +
		"\x04\x03\x04\x05\x04A\n\x04\x03\x05\x03\x05\x03\x05\x03\x05\x03\x05\x05" +
		"\x05H\n\x05\x03\x05\x03\x05\x03\x05\x03\x05\x03\x05\x03\x05\x05\x05P\n" +
		"\x05\x07\x05R\n\x05\f\x05\x0E\x05U\v\x05\x03\x06\x03\x06\x03\x07\x03\x07" +
		"\x03\b\x03\b\x03\b\x07\b^\n\b\f\b\x0E\ba\v\b\x03\t\x03\t\x03\n\x03\n\x05" +
		"\ng\n\n\x03\n\x07\nj\n\n\f\n\x0E\nm\v\n\x03\v\x03\v\x03\v\x03\v\x03\v" +
		"\x05\vt\n\v\x03\f\x03\f\x05\fx\n\f\x03\f\x03\f\x03\f\x03\f\x05\f~\n\f" +
		"\x03\f\x03\f\x03\f\x03\f\x05\f\x84\n\f\x07\f\x86\n\f\f\f\x0E\f\x89\v\f" +
		"\x03\f\x03\f\x03\r\x03\r\x03\r\x02\x02\x02\x0E\x02\x02\x04\x02\x06\x02" +
		"\b\x02\n\x02\f\x02\x0E\x02\x10\x02\x12\x02\x14\x02\x16\x02\x18\x02\x02" +
		"\x02\x02\x9E\x02\x1B\x03\x02\x02\x02\x04\'\x03\x02\x02\x02\x062\x03\x02" +
		"\x02\x02\bG\x03\x02\x02\x02\nV\x03\x02\x02\x02\fX\x03\x02\x02\x02\x0E" +
		"Z\x03\x02\x02\x02\x10b\x03\x02\x02\x02\x12f\x03\x02\x02\x02\x14n\x03\x02" +
		"\x02\x02\x16w\x03\x02\x02\x02\x18\x8C\x03\x02\x02\x02\x1A\x1C\x05\x04" +
		"\x03\x02\x1B\x1A\x03\x02\x02\x02\x1B\x1C\x03\x02\x02\x02\x1C \x03\x02" +
		"\x02\x02\x1D\x1F\x05\x18\r\x02\x1E\x1D\x03\x02\x02\x02\x1F\"\x03\x02\x02" +
		"\x02 \x1E\x03\x02\x02\x02 !\x03\x02\x02\x02!#\x03\x02\x02\x02\" \x03\x02" +
		"\x02\x02#$\x07\x02\x02\x03$\x03\x03\x02\x02\x02%(\x05\x10\t\x02&(\x05" +
		"\x0E\b\x02\'%\x03\x02\x02\x02\'&\x03\x02\x02\x02()\x03\x02\x02\x02)-\x07" +
		"\x03\x02\x02*,\x05\x06\x04\x02+*\x03\x02\x02\x02,/\x03\x02\x02\x02-+\x03" +
		"\x02\x02\x02-.\x03\x02\x02\x02.0\x03\x02\x02\x02/-\x03\x02\x02\x0201\x07" +
		"\x04\x02\x021\x05\x03\x02\x02\x022@\x07\x0E\x02\x0235\x07\x05\x02\x02" +
		"46\x05\n\x06\x0254\x03\x02\x02\x0256\x03\x02\x02\x0267\x03\x02\x02\x02" +
		"79\x07\x06\x02\x028:\x05\f\x07\x0298\x03\x02\x02\x029:\x03\x02\x02\x02" +
		":A\x03\x02\x02\x02;=\x07\x05\x02\x02<>\x05\n\x06\x02=<\x03\x02\x02\x02" +
		"=>\x03\x02\x02\x02>?\x03\x02\x02\x02?A\x07\x07\x02\x02@3\x03\x02\x02\x02" +
		"@;\x03\x02\x02\x02A\x07\x03\x02\x02\x02BH\x05\x10\t\x02CH\x05\x0E\b\x02" +
		"DH\x05\x12\n\x02EH\x05\x14\v\x02FH\x05\x16\f\x02GB\x03\x02\x02\x02GC\x03" +
		"\x02\x02\x02GD\x03\x02\x02\x02GE\x03\x02\x02\x02GF\x03\x02\x02\x02HS\x03" +
		"\x02\x02\x02IO\x07\b\x02\x02JP\x05\x10\t\x02KP\x05\x0E\b\x02LP\x05\x12" +
		"\n\x02MP\x05\x14\v\x02NP\x05\x16\f\x02OJ\x03\x02\x02\x02OK\x03\x02\x02" +
		"\x02OL\x03\x02\x02\x02OM\x03\x02\x02\x02ON\x03\x02\x02\x02PR\x03\x02\x02" +
		"\x02QI\x03\x02\x02\x02RU\x03\x02\x02\x02SQ\x03\x02\x02\x02ST\x03\x02\x02" +
		"\x02T\t\x03\x02\x02\x02US\x03\x02\x02\x02VW\x05\b\x05\x02W\v\x03\x02\x02" +
		"\x02XY\x05\b\x05\x02Y\r\x03\x02\x02\x02Z_\x07\x0E\x02\x02[\\\x07\t\x02" +
		"\x02\\^\x07\x0E\x02\x02][\x03\x02\x02\x02^a\x03\x02\x02\x02_]\x03\x02" +
		"\x02\x02_`\x03\x02\x02\x02`\x0F\x03\x02\x02\x02a_\x03\x02\x02\x02bc\x07" +
		"\x0E\x02\x02c\x11\x03\x02\x02\x02dg\x05\x10\t\x02eg\x05\x0E\b\x02fd\x03" +
		"\x02\x02\x02fe\x03\x02\x02\x02gk\x03\x02\x02\x02hj\x07\n\x02\x02ih\x03" +
		"\x02\x02\x02jm\x03\x02\x02\x02ki\x03\x02\x02\x02kl\x03\x02\x02\x02l\x13" +
		"\x03\x02\x02\x02mk\x03\x02\x02\x02no\x07\x0E\x02\x02os\x07\v\x02\x02p" +
		"t\x05\x10\t\x02qt\x05\x0E\b\x02rt\x05\x12\n\x02sp\x03\x02\x02\x02sq\x03" +
		"\x02\x02\x02sr\x03\x02\x02\x02t\x15\x03\x02\x02\x02ux\x05\x10\t\x02vx" +
		"\x05\x0E\b\x02wu\x03\x02\x02\x02wv\x03\x02\x02\x02xy\x03\x02\x02\x02y" +
		"}\x07\f\x02\x02z~\x05\x10\t\x02{~\x05\x0E\b\x02|~\x05\x16\f\x02}z\x03" +
		"\x02\x02\x02}{\x03\x02\x02\x02}|\x03\x02\x02\x02~\x87\x03\x02\x02\x02" +
		"\x7F\x83\x07\b\x02\x02\x80\x84\x05\x10\t\x02\x81\x84\x05\x0E\b\x02\x82" +
		"\x84\x05\x16\f\x02\x83\x80\x03\x02\x02\x02\x83\x81\x03\x02\x02\x02\x83" +
		"\x82\x03\x02\x02\x02\x84\x86\x03\x02\x02\x02\x85\x7F\x03\x02\x02\x02\x86" +
		"\x89\x03\x02\x02\x02\x87\x85\x03\x02\x02\x02\x87\x88\x03\x02\x02\x02\x88" +
		"\x8A\x03\x02\x02\x02\x89\x87\x03\x02\x02\x02\x8A\x8B\x07\r\x02\x02\x8B" +
		"\x17\x03\x02\x02\x02\x8C\x8D\x07\x0F\x02\x02\x8D\x19\x03\x02\x02\x02\x15" +
		"\x1B \'-59=@GOS_fksw}\x83\x87";
	public static __ATN: ATN;
	public static get _ATN(): ATN {
		if (!LQLParser.__ATN) {
			LQLParser.__ATN = new ATNDeserializer().deserialize(Utils.toCharArray(LQLParser._serializedATN));
		}

		return LQLParser.__ATN;
	}

}

export class ParseContext extends ParserRuleContext {
	public EOF(): TerminalNode { return this.getToken(LQLParser.EOF, 0); }
	public interfaceSpec(): InterfaceSpecContext | undefined {
		return this.tryGetRuleContext(0, InterfaceSpecContext);
	}
	public filter(): FilterContext[];
	public filter(i: number): FilterContext;
	public filter(i?: number): FilterContext | FilterContext[] {
		if (i === undefined) {
			return this.getRuleContexts(FilterContext);
		} else {
			return this.getRuleContext(i, FilterContext);
		}
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_parse; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterParse) {
			listener.enterParse(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitParse) {
			listener.exitParse(this);
		}
	}
}


export class InterfaceSpecContext extends ParserRuleContext {
	public simpletype(): SimpletypeContext | undefined {
		return this.tryGetRuleContext(0, SimpletypeContext);
	}
	public qualifiedtype(): QualifiedtypeContext | undefined {
		return this.tryGetRuleContext(0, QualifiedtypeContext);
	}
	public methodSig(): MethodSigContext[];
	public methodSig(i: number): MethodSigContext;
	public methodSig(i?: number): MethodSigContext | MethodSigContext[] {
		if (i === undefined) {
			return this.getRuleContexts(MethodSigContext);
		} else {
			return this.getRuleContext(i, MethodSigContext);
		}
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_interfaceSpec; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterInterfaceSpec) {
			listener.enterInterfaceSpec(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitInterfaceSpec) {
			listener.exitInterfaceSpec(this);
		}
	}
}


export class MethodSigContext extends ParserRuleContext {
	public NAME(): TerminalNode { return this.getToken(LQLParser.NAME, 0); }
	public inputs(): InputsContext | undefined {
		return this.tryGetRuleContext(0, InputsContext);
	}
	public outputs(): OutputsContext | undefined {
		return this.tryGetRuleContext(0, OutputsContext);
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_methodSig; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterMethodSig) {
			listener.enterMethodSig(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitMethodSig) {
			listener.exitMethodSig(this);
		}
	}
}


export class ParametersContext extends ParserRuleContext {
	public simpletype(): SimpletypeContext[];
	public simpletype(i: number): SimpletypeContext;
	public simpletype(i?: number): SimpletypeContext | SimpletypeContext[] {
		if (i === undefined) {
			return this.getRuleContexts(SimpletypeContext);
		} else {
			return this.getRuleContext(i, SimpletypeContext);
		}
	}
	public qualifiedtype(): QualifiedtypeContext[];
	public qualifiedtype(i: number): QualifiedtypeContext;
	public qualifiedtype(i?: number): QualifiedtypeContext | QualifiedtypeContext[] {
		if (i === undefined) {
			return this.getRuleContexts(QualifiedtypeContext);
		} else {
			return this.getRuleContext(i, QualifiedtypeContext);
		}
	}
	public arraytype(): ArraytypeContext[];
	public arraytype(i: number): ArraytypeContext;
	public arraytype(i?: number): ArraytypeContext | ArraytypeContext[] {
		if (i === undefined) {
			return this.getRuleContexts(ArraytypeContext);
		} else {
			return this.getRuleContext(i, ArraytypeContext);
		}
	}
	public namedparam(): NamedparamContext[];
	public namedparam(i: number): NamedparamContext;
	public namedparam(i?: number): NamedparamContext | NamedparamContext[] {
		if (i === undefined) {
			return this.getRuleContexts(NamedparamContext);
		} else {
			return this.getRuleContext(i, NamedparamContext);
		}
	}
	public typeparam(): TypeparamContext[];
	public typeparam(i: number): TypeparamContext;
	public typeparam(i?: number): TypeparamContext | TypeparamContext[] {
		if (i === undefined) {
			return this.getRuleContexts(TypeparamContext);
		} else {
			return this.getRuleContext(i, TypeparamContext);
		}
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_parameters; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterParameters) {
			listener.enterParameters(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitParameters) {
			listener.exitParameters(this);
		}
	}
}


export class InputsContext extends ParserRuleContext {
	public parameters(): ParametersContext {
		return this.getRuleContext(0, ParametersContext);
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_inputs; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterInputs) {
			listener.enterInputs(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitInputs) {
			listener.exitInputs(this);
		}
	}
}


export class OutputsContext extends ParserRuleContext {
	public parameters(): ParametersContext {
		return this.getRuleContext(0, ParametersContext);
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_outputs; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterOutputs) {
			listener.enterOutputs(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitOutputs) {
			listener.exitOutputs(this);
		}
	}
}


export class QualifiedtypeContext extends ParserRuleContext {
	public NAME(): TerminalNode[];
	public NAME(i: number): TerminalNode;
	public NAME(i?: number): TerminalNode | TerminalNode[] {
		if (i === undefined) {
			return this.getTokens(LQLParser.NAME);
		} else {
			return this.getToken(LQLParser.NAME, i);
		}
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_qualifiedtype; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterQualifiedtype) {
			listener.enterQualifiedtype(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitQualifiedtype) {
			listener.exitQualifiedtype(this);
		}
	}
}


export class SimpletypeContext extends ParserRuleContext {
	public NAME(): TerminalNode { return this.getToken(LQLParser.NAME, 0); }
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_simpletype; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterSimpletype) {
			listener.enterSimpletype(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitSimpletype) {
			listener.exitSimpletype(this);
		}
	}
}


export class ArraytypeContext extends ParserRuleContext {
	public simpletype(): SimpletypeContext | undefined {
		return this.tryGetRuleContext(0, SimpletypeContext);
	}
	public qualifiedtype(): QualifiedtypeContext | undefined {
		return this.tryGetRuleContext(0, QualifiedtypeContext);
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_arraytype; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterArraytype) {
			listener.enterArraytype(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitArraytype) {
			listener.exitArraytype(this);
		}
	}
}


export class NamedparamContext extends ParserRuleContext {
	public NAME(): TerminalNode { return this.getToken(LQLParser.NAME, 0); }
	public simpletype(): SimpletypeContext | undefined {
		return this.tryGetRuleContext(0, SimpletypeContext);
	}
	public qualifiedtype(): QualifiedtypeContext | undefined {
		return this.tryGetRuleContext(0, QualifiedtypeContext);
	}
	public arraytype(): ArraytypeContext | undefined {
		return this.tryGetRuleContext(0, ArraytypeContext);
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_namedparam; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterNamedparam) {
			listener.enterNamedparam(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitNamedparam) {
			listener.exitNamedparam(this);
		}
	}
}


export class TypeparamContext extends ParserRuleContext {
	public simpletype(): SimpletypeContext[];
	public simpletype(i: number): SimpletypeContext;
	public simpletype(i?: number): SimpletypeContext | SimpletypeContext[] {
		if (i === undefined) {
			return this.getRuleContexts(SimpletypeContext);
		} else {
			return this.getRuleContext(i, SimpletypeContext);
		}
	}
	public qualifiedtype(): QualifiedtypeContext[];
	public qualifiedtype(i: number): QualifiedtypeContext;
	public qualifiedtype(i?: number): QualifiedtypeContext | QualifiedtypeContext[] {
		if (i === undefined) {
			return this.getRuleContexts(QualifiedtypeContext);
		} else {
			return this.getRuleContext(i, QualifiedtypeContext);
		}
	}
	public typeparam(): TypeparamContext[];
	public typeparam(i: number): TypeparamContext;
	public typeparam(i?: number): TypeparamContext | TypeparamContext[] {
		if (i === undefined) {
			return this.getRuleContexts(TypeparamContext);
		} else {
			return this.getRuleContext(i, TypeparamContext);
		}
	}
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_typeparam; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterTypeparam) {
			listener.enterTypeparam(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitTypeparam) {
			listener.exitTypeparam(this);
		}
	}
}


export class FilterContext extends ParserRuleContext {
	public FILTERVALUE(): TerminalNode { return this.getToken(LQLParser.FILTERVALUE, 0); }
	constructor(parent: ParserRuleContext | undefined, invokingState: number) {
		super(parent, invokingState);
	}
	// @Override
	public get ruleIndex(): number { return LQLParser.RULE_filter; }
	// @Override
	public enterRule(listener: LQLListener): void {
		if (listener.enterFilter) {
			listener.enterFilter(this);
		}
	}
	// @Override
	public exitRule(listener: LQLListener): void {
		if (listener.exitFilter) {
			listener.exitFilter(this);
		}
	}
}


