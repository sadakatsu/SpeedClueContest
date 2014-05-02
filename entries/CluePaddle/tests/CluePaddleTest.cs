using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ClueSharp;
using ClueSharp.tests;
using NUnit.Framework;

namespace CluePaddle.tests
{
  class CluePaddleTest : ClueAITest<CluePaddle>
  {
    
    [Test]
    public void NonDisproversTest()
    {
      m_ai.Reset(4, 0, new List<Suspect>(), new List<Weapon>(), new List<Room>());
      var l = m_ai.NonDisprovers(3, 2).ToList();
      Assert.AreEqual(2, l.Count);

      l = m_ai.NonDisprovers(3, null).ToList();
      Assert.AreEqual(3, l.Count);

      l = m_ai.NonDisprovers(0, 2).ToList();
      Assert.AreEqual(1, l.Count);

      l = m_ai.NonDisprovers(1, 2).ToList();
      Assert.AreEqual(0, l.Count);
    }
  }
}
