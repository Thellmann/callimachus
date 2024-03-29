package org.callimachusproject.rewrite;

import static org.callimachusproject.util.PercentCodec.encode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Substitution {
	private static final Pattern NUMERIC = Pattern.compile("\\d\\d?");
	private static final Pattern NAMED_GROUP_PATTERN = Pattern
			.compile("\\(\\?<(\\w+)>");
	private static final Pattern EXPRESSIONS = Pattern
			.compile("\\{[\\+#\\./;\\?\\&]?(?:[A-Za-z0-9_%\\.:\\*]+,?)+\\}");
	private static final Pattern VARIABLE = Pattern
			.compile("([A-Za-z0-9_%][A-Za-z0-9_%\\.]*):?([0-9]*)(\\*)?");

	private static enum Expansion {
		/* {var} */
		SIMPLE(null, null, ',', true),
		/* {+var} */
		RESERVED('+', null, ',', false),
		/* {#var} */
		FRAGMENT('#', '#', ',', false),
		/* {.var} */
		DOT('.', '.', '.', true),
		/* {/var} */
		SEGMENT('/', '/', '/', true),
		/* {;var} */
		PATH(';', true, ';', true),
		/* {?var} */
		QUERY('?', false, '&', true),
		/* {&var} */
		CONTIN('&', false, '&', true);
		final Character operator;
		final String prefix;
		final boolean named;
		final boolean empty;
		final char separator;
		final boolean encode;

		Expansion(Character operator, Character prefix, char separator, boolean encode) {
			this.operator = operator;
			this.prefix = prefix == null ? "" : prefix.toString();
			this.named = false;
			this.empty = false;
			this.separator = separator;
			this.encode = encode;
		}

		Expansion(Character operator, boolean empty, char separator, boolean encode) {
			this.operator = operator;
			this.prefix = operator.toString();
			this.named = true;
			this.empty = empty;
			this.separator = separator;
			this.encode = encode;
		}
	}

	private final String regex;
	private final Pattern pattern;
	private final String template;
	private final List<String> groupNames;
	private final Set<String> variables = new LinkedHashSet<String>();

	public static Substitution compile(String regex, String template,
			String flags) {
		return new Substitution(regex, template, flags);
	}

	public static Substitution compile(String regex, String template) {
		return new Substitution(regex, template, "");
	}

	public static Substitution compile(String command) {
		int end = command.indexOf('\n');
		if (end < 0) {
			end = command.length();
		}
		int split = command.indexOf(' ');
		if (split >= 0 && split < end) {
			String pattern = command.substring(0, split);
			String replacement = command.substring(split + 1);
			return Substitution.compile(pattern, replacement);
		} else {
			return Substitution.compile(".*", command);
		}
	}

	private Substitution(String regex, String template, String flags) {
		this.regex = regex;
		this.template = template;
		this.groupNames = extractGroupNames(regex);
		this.pattern = Pattern.compile(regex, flags(flags));
		Matcher m = EXPRESSIONS.matcher(template);
		while (m.find()) {
			String expr = m.group();
			Matcher v = VARIABLE.matcher(expr);
			while (v.find()) {
				variables.add(v.group(1));
			}
		}
	}

	public String toString() {
		return regex + " " + template;
	}

	public String pattern() {
		return regex;
	}

	public String substitution() {
		return template;
	}

	public int flags() {
		return pattern.flags();
	}

	public boolean containsVariableName(String name) {
		return variables.contains(name);
	}

	public CharSequence replace(CharSequence input) {
		return replace(input, Collections.<String, String>emptyMap());
	}

	public CharSequence replace(CharSequence input, Map<String, ?> variables) {
		if (template.indexOf('{') < 0)
			return template;
		Matcher m = pattern.matcher(input);
		if (!m.find())
			return null;
		return substitute(m, variables);
	}

	private CharSequence substitute(Matcher m, Map<String, ?> variables) {
		StringBuilder sb = new StringBuilder(255);
		for (int i = 0, n = template.length(); i < n; i++) {
			String expr = getExpression(template, i);
			if (expr != null) {
				Expansion ex = getExpansion(expr);
				CharSequence value = values(expr, m, variables, ex);
				if (value != null) {
					sb.append(ex.prefix).append(value);
				}
				i += expr.length() - 1;
			} else {
				sb.append(template.charAt(i));
			}
		}
		return sb;
	}

	private String getExpression(String template, int i) {
		char chr = template.charAt(i);
		if (chr == '{' && i + 2 < template.length()) {
			int j = template.indexOf('}', i);
			if (j > i) {
				String expr = template.substring(i, j + 1);
				if (EXPRESSIONS.matcher(expr).matches())
					return expr;
			}
		}
		return null;
	}

	private Expansion getExpansion(String expr) {
		Character prefix = expr.charAt(1);
		for (Expansion ex : Expansion.values()) {
			if (prefix.equals(ex.operator)) {
				return ex;
			}
		}
		return Expansion.SIMPLE;
	}

	private CharSequence values(String expression, Matcher regex,
			Map<String, ?> variables, Expansion ex) {
		StringBuilder sb = null;
		Matcher m = VARIABLE.matcher(expression);
		while (m.find()) {
			String name = m.group(1);
			String max = m.group(2);
			int maxLength = max.length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(max);
			boolean explode = "*".equals(m.group(3));
			CharSequence value = value(name, regex, variables, maxLength, explode, ex);
			if (value != null) {
				if (sb == null) {
					sb = new StringBuilder();
				} else {
					sb.append(ex.separator);
				}
				if (ex.encode) {
					sb.append(value);
				} else {
					sb.append(value);
				}
			}
		}
		return sb;
	}

	private CharSequence value(String name, Matcher regex,
			Map<String, ?> variables, int maxLength, boolean explode,
			Expansion ex) {
		if (groupNames.contains(name)) {
			try {
				return inline(name, regex.group(name), maxLength, ex);
			} catch (IllegalArgumentException e) {
				// check qs parameter
			}
		}
		if (name.length() < 3 && NUMERIC.matcher(name).matches()) {
			int g = Integer.parseInt(name);
			if (regex.groupCount() >= g)
				return inline(name, regex.group(g), maxLength, ex);
		}
		Object o = variables.get(name);
		if (o == null) {
			return null;
		} else if (o instanceof CharSequence) {
			return inline(name, (CharSequence) o, maxLength, ex);
		} else if (o.getClass().isArray() && Array.getLength(o) == 0) {
			return null;
		} else if (o.getClass().isArray()) {
			StringBuilder sb = null;
			for (int i = 0; i < Array.getLength(o); i++) {
				String value = Array.get(o, i).toString();
				if (value != null) {
					if (sb == null) {
						sb = new StringBuilder();
						sb.append(inline(name, value, maxLength, ex));
					} else if (explode) {
						sb.append(ex.separator);
						sb.append(inline(name, value, maxLength, ex));
					} else {
						sb.append(',');
						sb.append(inline(null, value, maxLength, ex));
					}
				}
			}
			return sb;
		} else if (o instanceof Map) {
			StringBuilder sb = null;
			Map<?, ?> map = (Map<?, ?>) o;
			for (Map.Entry<?, ?> e : map.entrySet()) {
				String key = e.getKey().toString();
				String value = e.getValue().toString();
				if (explode) {
					if (sb == null) {
						sb = new StringBuilder();
					} else {
						sb.append(ex.separator);
					}
					sb.append(inline(null, key, maxLength, ex));
					sb.append('=');
					sb.append(inline(null, value, maxLength, ex));
				} else {
					if (sb == null) {
						sb = new StringBuilder();
						sb.append(inline(name, key, maxLength, ex));
					} else {
						sb.append(',');
						sb.append(inline(null, key, maxLength, ex));
					}
					sb.append(',');
					sb.append(inline(null, value, maxLength, ex));
				}
			}
			return sb;
		} else {
			return inline(name, o.toString(), maxLength, ex);
		}
	}

	private CharSequence inline(String name, CharSequence value, int maxLength, Expansion ex) {
		if (value.length() > maxLength) {
			value = value.subSequence(0, maxLength);
		}
		if (name != null && ex.named) {
			if (ex.empty && value.length() == 0) {
				return name;
			} else if (ex.encode) {
				return name + '=' + encode(value.toString());
			} else {
				return name + '=' + value;
			}
		} else if (ex.encode) {
			return encode(value.toString());
		} else {
			return value;
		}
	}

	private List<String> extractGroupNames(String namedPattern) {
		List<String> groupNames = new ArrayList<String>();
		Matcher matcher = NAMED_GROUP_PATTERN.matcher(namedPattern);
		while (matcher.find()) {
			groupNames.add(matcher.group(1));
		}
		return groupNames;
	}

	private int flags(String flags) {
		int f = 0;
		for (char c : flags.toCharArray()) {
			switch (c) {
			case 's':
				f |= Pattern.DOTALL;
				break;
			case 'm':
				f |= Pattern.MULTILINE;
				break;
			case 'i':
				f |= Pattern.CASE_INSENSITIVE;
				break;
			case 'x':
				f |= Pattern.COMMENTS;
				break;
			case 'd':
				f |= Pattern.UNIX_LINES;
				break;
			case 'u':
				f |= Pattern.UNICODE_CASE;
				break;
			}
		}
		return f;
	}

}