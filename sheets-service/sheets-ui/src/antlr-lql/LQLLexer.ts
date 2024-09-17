// Generated from ./LQL.g4 by ANTLR 4.9.0-SNAPSHOT


import { ATN } from "antlr4ts/atn/ATN";
import { ATNDeserializer } from "antlr4ts/atn/ATNDeserializer";
import { CharStream } from "antlr4ts/CharStream";
import { Lexer } from "antlr4ts/Lexer";
import { LexerATNSimulator } from "antlr4ts/atn/LexerATNSimulator";
import { NotNull } from "antlr4ts/Decorators";
import { Override } from "antlr4ts/Decorators";
import { RuleContext } from "antlr4ts/RuleContext";
import { Vocabulary } from "antlr4ts/Vocabulary";
import { VocabularyImpl } from "antlr4ts/VocabularyImpl";

import * as Utils from "antlr4ts/misc/Utils";


export class LQLLexer extends Lexer {
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

	// tslint:disable:no-trailing-whitespace
	public static readonly channelNames: string[] = [
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN",
	];

	// tslint:disable:no-trailing-whitespace
	public static readonly modeNames: string[] = [
		"DEFAULT_MODE",
	];

	public static readonly ruleNames: string[] = [
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "NAME", "FILTERVALUE", "TEXT", "SPACE",
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
	public static readonly VOCABULARY: Vocabulary = new VocabularyImpl(LQLLexer._LITERAL_NAMES, LQLLexer._SYMBOLIC_NAMES, []);

	// @Override
	// @NotNull
	public get vocabulary(): Vocabulary {
		return LQLLexer.VOCABULARY;
	}
	// tslint:enable:no-trailing-whitespace


	constructor(input: CharStream) {
		super(input);
		this._interp = new LexerATNSimulator(LQLLexer._ATN, this);
	}

	// @Override
	public get grammarFileName(): string { return "LQL.g4"; }

	// @Override
	public get ruleNames(): string[] { return LQLLexer.ruleNames; }

	// @Override
	public get serializedATN(): string { return LQLLexer._serializedATN; }

	// @Override
	public get channelNames(): string[] { return LQLLexer.channelNames; }

	// @Override
	public get modeNames(): string[] { return LQLLexer.modeNames; }

	public static readonly _serializedATN: string =
		"\x03\uC91D\uCABA\u058D\uAFBA\u4F53\u0607\uEA8B\uC241\x02\x11X\b\x01\x04" +
		"\x02\t\x02\x04\x03\t\x03\x04\x04\t\x04\x04\x05\t\x05\x04\x06\t\x06\x04" +
		"\x07\t\x07\x04\b\t\b\x04\t\t\t\x04\n\t\n\x04\v\t\v\x04\f\t\f\x04\r\t\r" +
		"\x04\x0E\t\x0E\x04\x0F\t\x0F\x04\x10\t\x10\x03\x02\x03\x02\x03\x03\x03" +
		"\x03\x03\x04\x03\x04\x03\x05\x03\x05\x03\x05\x03\x05\x03\x06\x03\x06\x03" +
		"\x07\x03\x07\x03\b\x03\b\x03\t\x03\t\x03\t\x03\n\x03\n\x03\v\x03\v\x03" +
		"\f\x03\f\x03\r\x03\r\x07\r=\n\r\f\r\x0E\r@\v\r\x03\x0E\x03\x0E\x07\x0E" +
		"D\n\x0E\f\x0E\x0E\x0EG\v\x0E\x03\x0F\x03\x0F\x07\x0FK\n\x0F\f\x0F\x0E" +
		"\x0FN\v\x0F\x03\x0F\x03\x0F\x03\x10\x06\x10S\n\x10\r\x10\x0E\x10T\x03" +
		"\x10\x03\x10\x02\x02\x02\x11\x03\x02\x03\x05\x02\x04\x07\x02\x05\t\x02" +
		"\x06\v\x02\x07\r\x02\b\x0F\x02\t\x11\x02\n\x13\x02\v\x15\x02\f\x17\x02" +
		"\r\x19\x02\x0E\x1B\x02\x0F\x1D\x02\x10\x1F\x02\x11\x03\x02\b\x06\x02&" +
		"&C\\aac|\x07\x02&&2;C\\aac|\b\x02##,,C\\^^aac|\n\x02$$)),,2<C\\^^`ac|" +
		"\x05\x02\f\f\x0F\x0F))\x05\x02\v\f\x0F\x0F\"\"\x02[\x02\x03\x03\x02\x02" +
		"\x02\x02\x05\x03\x02\x02\x02\x02\x07\x03\x02\x02\x02\x02\t\x03\x02\x02" +
		"\x02\x02\v\x03\x02\x02\x02\x02\r\x03\x02\x02\x02\x02\x0F\x03\x02\x02\x02" +
		"\x02\x11\x03\x02\x02\x02\x02\x13\x03\x02\x02\x02\x02\x15\x03\x02\x02\x02" +
		"\x02\x17\x03\x02\x02\x02\x02\x19\x03\x02\x02\x02\x02\x1B\x03\x02\x02\x02" +
		"\x02\x1D\x03\x02\x02\x02\x02\x1F\x03\x02\x02\x02\x03!\x03\x02\x02\x02" +
		"\x05#\x03\x02\x02\x02\x07%\x03\x02\x02\x02\t\'\x03\x02\x02\x02\v+\x03" +
		"\x02\x02\x02\r-\x03\x02\x02\x02\x0F/\x03\x02\x02\x02\x111\x03\x02\x02" +
		"\x02\x134\x03\x02\x02\x02\x156\x03\x02\x02\x02\x178\x03\x02\x02\x02\x19" +
		":\x03\x02\x02\x02\x1BA\x03\x02\x02\x02\x1DH\x03\x02\x02\x02\x1FR\x03\x02" +
		"\x02\x02!\"\x07}\x02\x02\"\x04\x03\x02\x02\x02#$\x07\x7F\x02\x02$\x06" +
		"\x03\x02\x02\x02%&\x07*\x02\x02&\b\x03\x02\x02\x02\'(\x07+\x02\x02()\x07" +
		"/\x02\x02)*\x07@\x02\x02*\n\x03\x02\x02\x02+,\x07+\x02\x02,\f\x03\x02" +
		"\x02\x02-.\x07.\x02\x02.\x0E\x03\x02\x02\x02/0\x070\x02\x020\x10\x03\x02" +
		"\x02\x0212\x07]\x02\x0223\x07_\x02\x023\x12\x03\x02\x02\x0245\x07?\x02" +
		"\x025\x14\x03\x02\x02\x0267\x07>\x02\x027\x16\x03\x02\x02\x0289\x07@\x02" +
		"\x029\x18\x03\x02\x02\x02:>\t\x02\x02\x02;=\t\x03\x02\x02<;\x03\x02\x02" +
		"\x02=@\x03\x02\x02\x02><\x03\x02\x02\x02>?\x03\x02\x02\x02?\x1A\x03\x02" +
		"\x02\x02@>\x03\x02\x02\x02AE\t\x04\x02\x02BD\t\x05\x02\x02CB\x03\x02\x02" +
		"\x02DG\x03\x02\x02\x02EC\x03\x02\x02\x02EF\x03\x02\x02\x02F\x1C\x03\x02" +
		"\x02\x02GE\x03\x02\x02\x02HL\x07)\x02\x02IK\n\x06\x02\x02JI\x03\x02\x02" +
		"\x02KN\x03\x02\x02\x02LJ\x03\x02\x02\x02LM\x03\x02\x02\x02MO\x03\x02\x02" +
		"\x02NL\x03\x02\x02\x02OP\x07)\x02\x02P\x1E\x03\x02\x02\x02QS\t\x07\x02" +
		"\x02RQ\x03\x02\x02\x02ST\x03\x02\x02\x02TR\x03\x02\x02\x02TU\x03\x02\x02" +
		"\x02UV\x03\x02\x02\x02VW\b\x10\x02\x02W \x03\x02\x02\x02\x07\x02>ELT\x03" +
		"\b\x02\x02";
	public static __ATN: ATN;
	public static get _ATN(): ATN {
		if (!LQLLexer.__ATN) {
			LQLLexer.__ATN = new ATNDeserializer().deserialize(Utils.toCharArray(LQLLexer._serializedATN));
		}

		return LQLLexer.__ATN;
	}

}

