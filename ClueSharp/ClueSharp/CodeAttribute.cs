using System;

namespace ClueSharp
{
  public class CodeAttribute : Attribute
  {
    private readonly string m_code;

    public CodeAttribute(string code)
    {
      m_code = code;
    }

    public string Code
    {
      get { return m_code; }
    }
  }
}
