using System;
using System.Collections.Generic;
using System.Linq;

namespace ClueSharp
{
  public class Card
  {
    private readonly Suspect m_suspect;
    private readonly Weapon m_weapon;
    private readonly Room m_room;

    public Card(object o)
      : this(o is Suspect ? (Suspect) o : Suspect.None,
             o is Weapon ? (Weapon) o : Weapon.None,
             o is Room ? (Room) o : Room.None)
    {
      if (m_suspect == Suspect.None && m_weapon == Weapon.None && m_room == Room.None)
      {
        throw new Exception("Constructed a card object without a valid card enum.");
      }
    }

    private Card(Suspect suspect, Weapon weapon, Room room)
    {
      m_suspect = suspect;
      m_weapon = weapon;
      m_room = room;
    }

    public override string ToString()
    {
      return EnumConversion.Convert(Value);
    }

    public override int GetHashCode()
    {
      return m_suspect.GetHashCode() + 99 * m_weapon.GetHashCode() + 99 * 99 * m_room.GetHashCode();
    }

    public override bool Equals(object obj)
    {
      // todo: use GetHashCode() ?
      var card = obj as Card;
      if (card == null) return false;
      return (m_suspect == card.m_suspect
              && m_weapon == card.m_weapon
              && m_room == card.m_room);
    }

    public static IEnumerable<Suspect> AllSuspects
    {
      get
      {
        return Enum.GetValues(typeof(Suspect))
          .Cast<Suspect>()
          .Where(suspect => suspect != Suspect.None && suspect != Suspect.Count);
      }
    }

    public static IEnumerable<Weapon> AllWeapons
    {
      get
      {
        return Enum.GetValues(typeof(Weapon))
          .Cast<Weapon>()
          .Where(weapon => weapon != Weapon.None && weapon != Weapon.Count);
      }
    }

    public static IEnumerable<Room> AllRooms
    {
      get
      {
        return Enum.GetValues(typeof(Room))
          .Cast<Room>()
          .Where(room => room != Room.None && room != Room.Count);
      }
    }

    public static IEnumerable<Enum> AllValues
    {
      get
      {
        foreach (var suspect in AllSuspects)
        {
          yield return suspect;
        }
        foreach (var weapon in AllWeapons)
        {
          yield return weapon;
        }
        foreach (var room in AllRooms)
        {
          yield return room;
        }
      }
    }

    public Enum Value
    {
      get
      {
        if (m_suspect != Suspect.None)
        {
          return m_suspect;
        }
        if (m_weapon != Weapon.None)
        {
          return m_weapon;
        }
        if (m_room != Room.None)
        {
          return m_room;
        }
        throw new Exception("Card doesn't have any valid values");
      }
    }
  }
}