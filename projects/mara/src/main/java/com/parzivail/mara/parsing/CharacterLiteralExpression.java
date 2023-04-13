package com.parzivail.mara.parsing;

import com.parzivail.mara.lexing.CharacterToken;

public class CharacterLiteralExpression extends Expression
{
	public final CharacterToken value;

	public CharacterLiteralExpression(CharacterToken value)
	{
		super(value);
		this.value = value;
	}
}
