using System;

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
      if (m_suspect != Suspect.None)
      {
        return EnumConversion.Convert(m_suspect);
      }
      if (m_weapon != Weapon.None)
      {
        return EnumConversion.Convert(m_weapon);
      }
      if (m_room != Room.None)
      {
        return EnumConversion.Convert(m_room);
      }
      throw new Exception("Card doesn't have any valid values");
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
  }
}