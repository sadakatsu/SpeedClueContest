using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;

namespace ClueSharp
{
  public class EnumConversion
  {
    private static readonly Dictionary<string, object> Lookup;
    private static readonly Dictionary<object, string> ReverseLookup; 

    static EnumConversion()
    {
      Lookup = new Dictionary<string, object>();
      ReverseLookup = new Dictionary<object, string>();

      AddValues<Suspect>();
      AddValues<Weapon>();
      AddValues<Room>();
    }

    private static void AddValues<T>()
    {
      var enumType = typeof (T);
      foreach (Enum val in Enum.GetValues(enumType))
      {
        FieldInfo fi = enumType.GetField(val.ToString());
        var attributes = (CodeAttribute[]) fi.GetCustomAttributes(
          typeof (CodeAttribute), false);
        if (!attributes.Any()) { continue; }
        CodeAttribute attr = attributes[0];
        var enumValue = (T) (object) val;
        Lookup[attr.Code] = enumValue;
        ReverseLookup[enumValue] = attr.Code;
      }
    }

    public static object ParseOneCard(string s)
    {
      if (!Lookup.ContainsKey(s))
        throw new ArgumentException();
      return Lookup[s];
    }

    internal static string Convert(Enum room)
    {
      return ReverseLookup[room];
    }
  }
}
